import express, { type Express, type Request } from "express";
import cors from "cors";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import pinoHttp from "pino-http";
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
  app.use(helmet());
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

  app.use(await createRoutes());
  app.use(notFoundHandler);
  app.use(errorHandler);

  return app;
}
