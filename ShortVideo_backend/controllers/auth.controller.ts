import type { Request, Response } from "express";
import { AppError } from "../middleware/errorHandler";
import { asyncHandler } from "../utils/asyncHandler";
import {
  confirmPasswordReset,
  deleteAccount,
  loginUser,
  logoutUser,
  refreshUserSession,
  registerUser,
  requestPasswordReset,
} from "../service/auth.service";
import {
  loginSchema,
  logoutSchema,
  passwordResetConfirmSchema,
  passwordResetRequestSchema,
  refreshSchema,
  registerSchema,
} from "../validators/auth.schema";
import { getClientDeviceInfo } from "../utils/device";
import type { z } from "zod";

function parseBody<T>(schema: z.ZodSchema<T>, body: unknown): T {
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    const message = parsed.error.issues.map((issue) => issue.message).join(", ");
    throw new AppError(400, "VALIDATION_ERROR", message);
  }
  return parsed.data;
}

function sendData(res: Response, data: unknown, statusCode = 200): void {
  res.status(statusCode).json({
    data,
    request_id: res.getHeader("X-Request-Id"),
  });
}

export const postRegister = asyncHandler(async (req: Request, res: Response) => {
  const body = parseBody(registerSchema, req.body);
  const session = await registerUser(body, getClientDeviceInfo(req));
  sendData(res, session, 201);
});

export const postLogin = asyncHandler(async (req: Request, res: Response) => {
  const body = parseBody(loginSchema, req.body);
  const session = await loginUser(body, getClientDeviceInfo(req));
  sendData(res, session);
});

export const postRefresh = asyncHandler(async (req: Request, res: Response) => {
  const body = parseBody(refreshSchema, req.body);
  const session = await refreshUserSession(body.refreshToken);
  sendData(res, session);
});

export const postLogout = asyncHandler(async (req: Request, res: Response) => {
  const body = parseBody(logoutSchema, req.body);
  await logoutUser(body.refreshToken);
  sendData(res, { success: true });
});

export const postPasswordResetRequest = asyncHandler(async (req: Request, res: Response) => {
  const body = parseBody(passwordResetRequestSchema, req.body);
  const result = await requestPasswordReset(body.email);
  sendData(res, result);
});

export const postPasswordResetConfirm = asyncHandler(async (req: Request, res: Response) => {
  const body = parseBody(passwordResetConfirmSchema, req.body);
  await confirmPasswordReset(body);
  sendData(res, { success: true });
});

export const deleteAccountHandler = asyncHandler(async (req: Request, res: Response) => {
  if (!req.userId) {
    throw new AppError(401, "UNAUTHORIZED", "Authentication is required.");
  }

  const refreshToken =
    typeof req.body?.refreshToken === "string" ? req.body.refreshToken : undefined;

  await deleteAccount(req.userId, refreshToken);
  sendData(res, { success: true });
});
