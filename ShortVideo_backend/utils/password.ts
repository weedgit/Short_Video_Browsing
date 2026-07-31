import bcrypt from "bcryptjs";

const PASSWORD_ROUNDS = 12;

export async function hashPassword(plainText: string): Promise<string> {
  return bcrypt.hash(plainText, PASSWORD_ROUNDS);
}

export async function verifyPassword(plainText: string, passwordHash: string): Promise<boolean> {
  return bcrypt.compare(plainText, passwordHash);
}
