import type { Request, Response } from "express";
import { config } from "../config";
import { asyncHandler } from "../utils/asyncHandler";
import { getApiInfo, getLiveHealth, getReadyHealth } from "../service/health.service";

export const getLive = asyncHandler(async (_req: Request, res: Response) => {
  res.status(200).json(getLiveHealth());
});

export const getReady = asyncHandler(async (_req: Request, res: Response) => {
  const result = await getReadyHealth();
  res.status(result.status === "ok" ? 200 : 503).json(result);
});

export const getVersion = asyncHandler(async (_req: Request, res: Response) => {
  res.status(200).json(getApiInfo(config.apiVersion));
});
