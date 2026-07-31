import type { NextFunction, Request, Response } from "express";
import { AppError } from "./errorHandler";

export function requireAdmin(req: Request, _res: Response, next: NextFunction): void {
  if (req.userRole !== "ADMIN") {
    next(new AppError(403, "FORBIDDEN", "Admin privileges are required."));
    return;
  }

  next();
}
