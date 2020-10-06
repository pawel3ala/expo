package host.exp.exponent.fcm

import android.annotation.SuppressLint
import expo.modules.notifications.service.NotificationsService

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class ExpoFcmMessagingService : NotificationsService() {
  override val firebaseMessagingDelegate by lazy { ExpoFirebaseMessagingDelegate(this) }
}
