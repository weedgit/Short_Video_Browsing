import { logger } from "../utils/logger";

export type PushNotificationPayload = {
  userId: string;
  title: string;
  body: string;
  fcmTokens: string[];
};

/**
 * Stub push sender: no firebase-admin dependency is wired up yet, so we just
 * log the intent. Notifications are always persisted in the database
 * regardless of whether a push could be delivered.
 */
export async function sendPushNotification(payload: PushNotificationPayload): Promise<void> {
  if (payload.fcmTokens.length === 0) {
    return;
  }

  logger.info(
    {
      userId: payload.userId,
      tokenCount: payload.fcmTokens.length,
      title: payload.title,
    },
    "Stub push notification dispatch (FCM integration not configured)",
  );
}
