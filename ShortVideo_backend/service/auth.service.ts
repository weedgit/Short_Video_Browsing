import { AppError } from "../middleware/errorHandler";
import {
  createUser,
  findUserByEmail,
  findUserById,
  findUserByUsername,
  isUserLoginAllowed,
  softDeleteUser,
  updateUserPassword,
} from "../db/repositories/user.repository";
import {
  findValidRefreshToken,
  revokeAllUserRefreshTokens,
  revokeRefreshToken,
} from "../db/repositories/refreshToken.repository";
import {
  createPasswordResetToken,
  findValidPasswordResetToken,
  markPasswordResetTokenUsed,
  revokeAllUserPasswordResetTokens,
} from "../db/repositories/passwordReset.repository";
import { upsertUserDevice, revokeUserDevices } from "../db/repositories/userDevice.repository";
import { hashPassword, verifyPassword } from "../utils/password";
import { generateSecureToken } from "../utils/token";
import { addDurationToDate } from "../utils/duration";
import { issueAuthSession, rotateAuthSession } from "./auth-token.service";
import type { AuthSessionResponse } from "../models/auth.types";
import { config } from "../config";
import type { ClientDeviceInfo } from "../utils/device";

export type RegisterInput = {
  email: string;
  password: string;
  username: string;
  displayName: string;
};

export type LoginInput = {
  email: string;
  password: string;
};

async function trackUserDevice(userId: string, device?: ClientDeviceInfo): Promise<void> {
  if (!device?.deviceId) {
    return;
  }

  await upsertUserDevice({
    userId,
    deviceId: device.deviceId,
    platform: device.platform,
    appVersion: device.appVersion,
  });
}

export async function registerUser(
  input: RegisterInput,
  device?: ClientDeviceInfo,
): Promise<AuthSessionResponse> {
  const email = input.email.trim().toLowerCase();
  const username = input.username.trim().toLowerCase();

  if (await findUserByEmail(email)) {
    throw new AppError(409, "EMAIL_ALREADY_EXISTS", "Email is already in use.");
  }

  if (await findUserByUsername(username)) {
    throw new AppError(409, "USERNAME_ALREADY_EXISTS", "Username is already in use.");
  }

  const passwordHash = await hashPassword(input.password);
  const user = await createUser({
    email,
    username,
    displayName: input.displayName.trim(),
    passwordHash,
  });

  const session = await issueAuthSession(user);
  await trackUserDevice(user.id, device);
  return session;
}

export async function loginUser(
  input: LoginInput,
  device?: ClientDeviceInfo,
): Promise<AuthSessionResponse> {
  const user = await findUserByEmail(input.email.trim().toLowerCase());

  if (!user || !(await verifyPassword(input.password, user.passwordHash))) {
    throw new AppError(401, "INVALID_CREDENTIALS", "Email or password is incorrect.");
  }

  if (!isUserLoginAllowed(user)) {
    throw new AppError(403, "USER_NOT_ACTIVE", "This account is not available.");
  }

  const session = await issueAuthSession(user);
  await trackUserDevice(user.id, device);
  return session;
}

export async function refreshUserSession(refreshToken: string): Promise<AuthSessionResponse> {
  const storedToken = await findValidRefreshToken(refreshToken);
  if (!storedToken) {
    throw new AppError(401, "INVALID_REFRESH_TOKEN", "Invalid refresh token.");
  }

  const user = await findUserById(storedToken.userId);
  if (!user || !isUserLoginAllowed(user)) {
    throw new AppError(401, "INVALID_REFRESH_TOKEN", "Invalid refresh token.");
  }

  try {
    return await rotateAuthSession(user, refreshToken);
  } catch {
    throw new AppError(401, "INVALID_REFRESH_TOKEN", "Invalid refresh token.");
  }
}

export async function logoutUser(refreshToken: string): Promise<void> {
  await revokeRefreshToken(refreshToken);
}

export async function requestPasswordReset(email: string): Promise<{ message: string; resetToken?: string }> {
  const user = await findUserByEmail(email.trim().toLowerCase());

  if (!user || !isUserLoginAllowed(user)) {
    return { message: "Password reset instructions have been sent." };
  }

  const resetToken = generateSecureToken();
  await createPasswordResetToken({
    userId: user.id,
    token: resetToken,
    expiresAt: addDurationToDate("1h"),
  });

  if (!config.isProduction) {
    return {
      message: "Password reset token has been generated.",
      resetToken,
    };
  }

  return { message: "Password reset instructions have been sent." };
}

export async function confirmPasswordReset(input: {
  token: string;
  newPassword: string;
}): Promise<void> {
  const storedToken = await findValidPasswordResetToken(input.token);
  if (!storedToken) {
    throw new AppError(400, "INVALID_RESET_TOKEN", "Reset token is invalid or expired.");
  }

  const user = await findUserById(storedToken.userId);
  if (!user || !isUserLoginAllowed(user)) {
    throw new AppError(400, "INVALID_RESET_TOKEN", "Reset token is invalid or expired.");
  }

  const passwordHash = await hashPassword(input.newPassword);
  await updateUserPassword(user.id, passwordHash);
  await markPasswordResetTokenUsed(input.token);
  await revokeAllUserRefreshTokens(user.id);
}

export async function deleteAccount(userId: string, refreshToken?: string): Promise<void> {
  const user = await findUserById(userId);
  if (!user || user.status === "DELETED") {
    throw new AppError(404, "USER_NOT_FOUND", "User not found.");
  }

  await softDeleteUser(userId);
  await revokeAllUserRefreshTokens(userId);
  await revokeAllUserPasswordResetTokens(userId);
  await revokeUserDevices(userId);

  if (refreshToken) {
    await revokeRefreshToken(refreshToken);
  }
}
