import type { User, UserStatus } from "@prisma/client";
import { getPrismaClient } from "../client";

export type CreateUserInput = {
  email: string;
  passwordHash: string;
  username: string;
  displayName: string;
};

export async function findUserByEmail(email: string): Promise<User | null> {
  return getPrismaClient().user.findUnique({
    where: { email: email.toLowerCase() },
  });
}

export async function findUserByUsername(username: string): Promise<User | null> {
  return getPrismaClient().user.findUnique({
    where: { username: username.toLowerCase() },
  });
}

export async function findUserById(id: string): Promise<User | null> {
  return getPrismaClient().user.findUnique({ where: { id } });
}

export async function createUser(input: CreateUserInput): Promise<User> {
  return getPrismaClient().user.create({
    data: {
      email: input.email.toLowerCase(),
      passwordHash: input.passwordHash,
      username: input.username.toLowerCase(),
      displayName: input.displayName,
    },
  });
}

export async function updateUserPassword(userId: string, passwordHash: string): Promise<User> {
  return getPrismaClient().user.update({
    where: { id: userId },
    data: { passwordHash },
  });
}

export async function updateUserProfileFields(
  userId: string,
  data: {
    displayName?: string;
    bio?: string | null;
    avatarUrl?: string | null;
  },
): Promise<User> {
  return getPrismaClient().user.update({
    where: { id: userId },
    data: {
      ...(data.displayName !== undefined ? { displayName: data.displayName } : {}),
      ...(data.bio !== undefined ? { bio: data.bio } : {}),
      ...(data.avatarUrl !== undefined ? { avatarUrl: data.avatarUrl } : {}),
    },
  });
}

export async function softDeleteUser(userId: string): Promise<User> {
  return getPrismaClient().user.update({
    where: { id: userId },
    data: {
      status: "DELETED",
      deletedAt: new Date(),
    },
  });
}

export function isUserLoginAllowed(user: User): boolean {
  return user.status === ("ACTIVE" satisfies UserStatus);
}
