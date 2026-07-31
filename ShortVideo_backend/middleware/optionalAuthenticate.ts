import type { NextFunction, Request, Response } from "express";
import { verifyAccessToken } from "../service/auth-token.service";

export function optionalAuthenticate(req: Request, _res: Response, next: NextFunction): void {
  const header = req.headers.authorization;

  if (!header?.startsWith("Bearer ")) {
    next();
    return;
  }

  const token = header.slice("Bearer ".length).trim();
  if (!token) {
    next();
    return;
  }

  try {
    const claims = verifyAccessToken(token);
    req.userId = claims.sub;
    req.userEmail = claims.email;
    req.userRole = claims.role;
  } catch {
    // Ignore invalid tokens for optional auth routes.
  }

  next();
}
