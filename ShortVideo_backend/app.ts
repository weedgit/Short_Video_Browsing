import express, { type Express, type Request } from "express";
import cors from "cors";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import pinoHttp from "pino-http";
import path from "node:path";
import { mkdirSync } from "node:fs";
import { config } from "./config";
import { errorHandler, notFoundHandler } from "./middleware/errorHandler";
import { requestIdMiddleware } from "./middleware/requestId";
import { createRoutes } from "./routes";
import { logger } from "./utils/logger";

export async function createApp(): Promise<Express> {
  const app = express();

  app.disable("x-powered-by");
  app.set("trust proxy", 1);

  app.use(requestIdMiddleware);
  app.use(
    pinoHttp({
      logger,
      genReqId: (req) => req.requestId ?? "unknown",
    }),
  );
  app.use(
    helmet({
      // Admin UI runs on another origin/port (e.g. localhost:5173).
      crossOriginResourcePolicy: { policy: "cross-origin" },
    }),
  );
  app.use(
    cors({
      origin: config.corsOrigin === "*" ? true : config.corsOrigin,
      credentials: true,
    }),
  );
  app.use(express.json({
    limit: "1mb",
    verify: (req, _res, buf) => {
      (req as Request).rawBody = buf;
    },
  }));
  app.use(
    rateLimit({
      windowMs: 60_000,
      max: 300,
      standardHeaders: true,
      legacyHeaders: false,
    }),
  );

  const avatarDir = path.resolve(process.cwd(), "storage", "avatars");
  mkdirSync(avatarDir, { recursive: true });
  app.use("/avatars", express.static(avatarDir));

  app.use(await createRoutes());
  app.use(notFoundHandler);
  app.use(errorHandler);

  return app;
}
