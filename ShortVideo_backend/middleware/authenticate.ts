import type { NextFunction, Request, Response } from "express";
import { AppError } from "./errorHandler";
import { verifyAccessToken } from "../service/auth-token.service";

export function authenticate(req: Request, _res: Response, next: NextFunction): void {
  const header = req.headers.authorization;

  if (!header?.startsWith("Bearer ")) {
    next(new AppError(401, "UNAUTHORIZED", "Authentication is required."));
    return;
  }

  const token = header.slice("Bearer ".length).trim();
  if (!token) {
    next(new AppError(401, "UNAUTHORIZED", "Authentication is required."));
    return;
  }

  try {
    const claims = verifyAccessToken(token);
    req.userId = claims.sub;
    req.userEmail = claims.email;
    req.userRole = claims.role;
    next();
  } catch {
    next(new AppError(401, "UNAUTHORIZED", "Invalid access token."));
  }
}
