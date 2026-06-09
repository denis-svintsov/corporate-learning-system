import { useAuth } from "@/contexts/AuthContext";
import { buildNotificationStreamUrl } from "@/lib/notificationsApi";
import { useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";

export function NotificationStream() {
  const { token, isAuthenticated } = useAuth();
  const queryClient = useQueryClient();

  useEffect(() => {
    if (!isAuthenticated || !token) {
      return;
    }

    const source = new EventSource(buildNotificationStreamUrl(token));

    const refreshNotifications = () => {
      queryClient.invalidateQueries({ queryKey: ["notifications"] });
      queryClient.invalidateQueries({ queryKey: ["notifications-unread-count"] });
      queryClient.invalidateQueries({ queryKey: ["assignment-requests-pending-count"] });
      queryClient.invalidateQueries({ queryKey: ["assignment-requests-admin"] });
    };

    source.addEventListener("notification", refreshNotifications);

    return () => {
      source.removeEventListener("notification", refreshNotifications);
      source.close();
    };
  }, [isAuthenticated, queryClient, token]);

  return null;
}
