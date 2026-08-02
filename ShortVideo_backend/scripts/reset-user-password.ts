import { PrismaClient } from "@prisma/client";
import { hashPassword } from "../utils/password";

const emailArg = process.argv[2];
const password = process.argv[3] ?? "Password123!";

if (!emailArg) {
  console.error("Usage: npx tsx scripts/reset-user-password.ts <email> [password]");
  process.exit(1);
}

const email = emailArg.toLowerCase();
const prisma = new PrismaClient();

async function main() {
  const user = await prisma.user.findUnique({ where: { email } });
  if (!user) {
    console.error(`User not found: ${email}`);
    process.exitCode = 1;
    return;
  }

  const passwordHash = await hashPassword(password);
  await prisma.user.update({
    where: { id: user.id },
    data: { passwordHash, status: "ACTIVE" },
  });
  console.log(`Reset password for ${user.email} (${user.username})`);
  console.log(`New password: ${password}`);
}

main()
  .catch((error) => {
    console.error(error);
    process.exitCode = 1;
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
