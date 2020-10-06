package host.exp.exponent.fcm

import android.content.Context
import com.google.firebase.messaging.RemoteMessage
import expo.modules.notifications.notifications.model.NotificationContent
import expo.modules.notifications.notifications.model.NotificationRequest
import expo.modules.notifications.notifications.model.triggers.FirebaseNotificationTrigger
import expo.modules.notifications.service.delegates.FirebaseMessagingDelegate
import host.exp.exponent.ABIVersion
import host.exp.exponent.Constants
import host.exp.exponent.analytics.EXL
import host.exp.exponent.kernel.ExperienceId
import host.exp.exponent.notifications.PushNotificationHelper
import host.exp.exponent.notifications.model.ScopedNotificationRequest
import host.exp.exponent.storage.ExponentDB
import org.json.JSONException
import org.json.JSONObject

class ExpoFirebaseMessagingDelegate : FirebaseMessagingDelegate() {
  override fun onNewToken(context: Context, token: String) {
    if (!Constants.FCM_ENABLED) {
      return
    }
    super.onNewToken(context, token)
    FcmRegistrationIntentService.registerForeground(context.applicationContext, token)
  }

  override fun onMessageReceived(context: Context, remoteMessage: RemoteMessage) {
    if (!Constants.FCM_ENABLED) {
      return
    }
    val experienceDBObject = ExponentDB.experienceIdToExperienceSync(remoteMessage.data["experienceId"])
    if (experienceDBObject != null) {
      try {
        val manifest = JSONObject(experienceDBObject.manifest)
        val sdkVersion = ABIVersion.toNumber(manifest.getString("sdkVersion")) / 10000

        // If an experience is on SDK newer than 39, that is SDK40 and beyond up till UNVERSIONED
        // we only use the new notifications API as it is going to be removed from SDK40.
        if (sdkVersion >= 40) {
          dispatchToNextNotificationModule(context, remoteMessage)
          return
        } else if (sdkVersion == 38 || sdkVersion == 39) {
          // In SDK38 and 39 we want to let people decide which notifications API to use,
          // the next or the legacy one.
          val androidSection = manifest.optJSONObject("android")
          if (androidSection != null) {
            val useNextNotificationsApi = androidSection.optBoolean("useNextNotificationsApi", false)
            if (useNextNotificationsApi) {
              dispatchToNextNotificationModule(context, remoteMessage)
              return
            }
          }
        }
        // If it's an older experience or useNextNotificationsApi is set to false, let's use the legacy notifications API
        dispatchToLegacyNotificationModule(context, remoteMessage)
      } catch (e: JSONException) {
        e.printStackTrace()
        EXL.e("expo-notifications", "Couldn't parse the manifest.")
      }
    } else {
      EXL.e("expo-notifications", "No experience found for id ${remoteMessage.data["experienceId"]}")
    }
  }

  private fun dispatchToNextNotificationModule(context: Context, remoteMessage: RemoteMessage) {
    super.onMessageReceived(context, remoteMessage)
  }

  private fun dispatchToLegacyNotificationModule(context: Context, remoteMessage: RemoteMessage) {
    PushNotificationHelper.getInstance().onMessageReceived(
      context,
      remoteMessage.data["experienceId"],
      remoteMessage.data["channelId"],
      remoteMessage.data["message"],
      remoteMessage.data["body"],
      remoteMessage.data["title"],
      remoteMessage.data["categoryId"]
    )
  }

  override fun createNotificationRequest(identifier: String, content: NotificationContent, notificationTrigger: FirebaseNotificationTrigger): NotificationRequest {
    val data = notificationTrigger.remoteMessage.data
    val experienceId = if (data.containsKey("experienceId")) {
      ExperienceId.create(data["experienceId"])
    } else null
    return ScopedNotificationRequest(identifier, content, notificationTrigger, experienceId?.get())
  }
}
