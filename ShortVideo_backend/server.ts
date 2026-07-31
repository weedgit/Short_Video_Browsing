import { createServer, type Server } from "http";
import { createApp } from "./app";
import { config } from "./config";
import { disconnectPrisma } from "./db/client";
import { disconnectRedis } from "./integrations/redis";
import { startUploadWorkers, stopUploadWorkers } from "./jobs/queues";
import { logger } from "./utils/logger";

let server: Server;

async function startServer(): Promise<void> {
  const app = await createApp();
  server = createServer(app);

  server.listen(config.port, () => {
    startUploadWorkers();
    logger.info(
      {
        port: config.port,
        env: config.env,
        apiVersion: config.apiVersion,
      },
      "API server started",
    );
  });
}

function shutdown(signal: string): void {
  logger.info({ signal }, "Graceful shutdown started");

  server.close(async (err) => {
    if (err) {
      logger.error({ err }, "Error during server shutdown");
      process.exit(1);
    }

    try {
      await stopUploadWorkers();
      await disconnectPrisma();
      await disconnectRedis();
    } catch (disconnectError) {
      logger.error({ err: disconnectError }, "Error disconnecting Prisma");
      process.exit(1);
    }

    logger.info("Server closed");
    process.exit(0);
  });

  setTimeout(() => {
    logger.error("Forced shutdown after timeout");
    process.exit(1);
  }, 10_000).unref();
}

void startServer();

process.on("SIGTERM", () => shutdown("SIGTERM"));
process.on("SIGINT", () => shutdown("SIGINT"));

export {};
