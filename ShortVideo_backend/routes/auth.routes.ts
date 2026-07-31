import { Router, type RequestHandler } from "express";
import {
  postLogin,
  postLogout,
  postPasswordResetConfirm,
  postPasswordResetRequest,
  postRefresh,
  postRegister,
} from "../controllers/auth.controller";
import { createAuthRateLimiterWithStore } from "../middleware/createAuthRateLimiter";

function createNoOpRateLimiter(): RequestHandler {
  return (_req, _res, next) => {
    next();
  };
}

export async function createAuthRouter(): Promise<Router> {
  const authRouter = Router();
  const authRateLimiter =
    process.env.NODE_ENV === "test"
      ? createNoOpRateLimiter()
      : await createAuthRateLimiterWithStore();

  authRouter.use(authRateLimiter);
  authRouter.post("/register", postRegister);
  authRouter.post("/login", postLogin);
  authRouter.post("/refresh", postRefresh);
  authRouter.post("/logout", postLogout);
  authRouter.post("/password/reset/request", postPasswordResetRequest);
  authRouter.post("/password/reset/confirm", postPasswordResetConfirm);

  return authRouter;
}
