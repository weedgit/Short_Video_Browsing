import { Router } from "express";
import { getLive, getReady, getVersion } from "../controllers/health.controller";

const healthRouter = Router();

healthRouter.get("/live", getLive);
healthRouter.get("/ready", getReady);

export { healthRouter };

export const apiRouter = Router();
apiRouter.get("/", getVersion);
