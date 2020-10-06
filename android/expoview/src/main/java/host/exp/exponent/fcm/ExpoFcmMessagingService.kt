package host.exp.exponent.fcm

import android.annotation.SuppressLint
import expo.modules.notifications.service.NotificationsService

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class ExpoFcmMessagingService : NotificationsService(ExpoFirebaseMessagingDelegate())
