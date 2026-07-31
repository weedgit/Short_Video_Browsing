import { Router } from "express";
import swaggerUi from "swagger-ui-express";
import openapiSpec from "../docs/openapi.json";

export function createDocsRouter(): Router {
  const router = Router();

  router.get("/openapi.json", (_req, res) => {
    res.json(openapiSpec);
  });

  router.use("/docs", swaggerUi.serve, swaggerUi.setup(openapiSpec));

  return router;
}
