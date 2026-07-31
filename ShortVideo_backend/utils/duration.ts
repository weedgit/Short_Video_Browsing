export function parseDurationToSeconds(value: string): number {
  const match = value.trim().match(/^(\d+)([smhd])$/i);
  if (!match) {
    throw new Error(`Invalid duration format: ${value}`);
  }

  const amount = Number(match[1]);
  const unit = match[2]?.toLowerCase();

  switch (unit) {
    case "s":
      return amount;
    case "m":
      return amount * 60;
    case "h":
      return amount * 60 * 60;
    case "d":
      return amount * 60 * 60 * 24;
    default:
      throw new Error(`Unsupported duration unit: ${unit}`);
  }
}

export function addDurationToDate(value: string, from = new Date()): Date {
  return new Date(from.getTime() + parseDurationToSeconds(value) * 1000);
}
