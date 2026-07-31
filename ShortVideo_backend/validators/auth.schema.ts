import { z } from "zod";

export const registerSchema = z.object({
  email: z.string().trim().email("Enter a valid email address."),
  password: z.string().min(8, "Password must be at least 8 characters."),
  username: z
    .string()
    .trim()
    .min(3, "Username must be at least 3 characters.")
    .max(30, "Username must be at most 30 characters.")
    .regex(/^[a-zA-Z0-9_]+$/, "Username may only contain letters, numbers, and underscores."),
  displayName: z.string().trim().min(1, "Enter a display name.").max(50),
});

export const loginSchema = z.object({
  email: z.string().trim().email("Enter a valid email address."),
  password: z.string().min(1, "Enter your password."),
});

export const refreshSchema = z.object({
  refreshToken: z.string().min(1, "refreshToken is required."),
});

export const logoutSchema = refreshSchema;

export const passwordResetRequestSchema = z.object({
  email: z.string().trim().email("Enter a valid email address."),
});

export const passwordResetConfirmSchema = z.object({
  token: z.string().min(1, "Reset token is required."),
  newPassword: z.string().min(8, "Password must be at least 8 characters."),
});

export const deleteAccountSchema = z.object({
  refreshToken: z.string().optional(),
});
