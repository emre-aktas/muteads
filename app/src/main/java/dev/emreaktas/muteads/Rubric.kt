package dev.emreaktas.muteads

import org.json.JSONObject

/**
 * The questions Jev answers for every notification.
 *
 * Two gates on purpose: is_promo has to be high AND is_needed has to be low before
 * anything is dismissed. A bank's loan campaign is promotional, but the same app
 * also sends one-time codes -- a single question would be too blunt to tell them
 * apart safely.
 *
 * Kept deliberately short. The rubric is re-sent with every notification, so every
 * extra sentence here is paid for on each call, in tokens and in upload time.
 */
object Rubric {

    fun questions(): JSONObject = JSONObject().apply {
        put(
            "is_promo", JSONObject()
                .put("type", "noul")
                .put(
                    "instructions",
                    "Commercial outreach: the notification exists to get the user to buy, " +
                        "browse, subscribe or come back, rather than to inform them about " +
                        "something already underway. This covers discounts, sales, campaigns, " +
                        "coupons, limited-time offers, loyalty points, new collections, new " +
                        "seasons, product launches and new arrivals, invitations to shop or " +
                        "explore a catalogue, unrequested offers for a product, loan, card, " +
                        "plan or subscription, and algorithmic recommendations the user did " +
                        "not ask for. It equally covers re-engagement nudges that sell " +
                        "nothing: streak reminders, progress or activity prompts, and " +
                        "messages whose real purpose is to pull the user back into the app."
                )
        )

        put(
            "is_needed", JSONObject()
                .put("type", "noul")
                .put(
                    "instructions",
                    "Something the user needs: a one-time or verification code, a bank or " +
                        "card transaction, an order confirmation, receipt, shipping or " +
                        "delivery update, a ride or flight status, an appointment or calendar " +
                        "reminder, a security alert, a device or system event, or a message " +
                        "written by a real person to this user. Automated or bot-generated " +
                        "marketing does not count."
                )
        )

        put(
            "category", JSONObject()
                .put("type", "choice")
                .put("instructions", "What kind of notification this is")
                .put(
                    "criteria", JSONObject()
                        .put("promo", "Marketing or a nudge to reopen the app")
                        .put("transactional", "Code, payment, order, delivery or booking")
                        .put("personal", "A message from a real person")
                        .put("system", "Device, storage or app update")
                        .put("content", "A new post, video or article")
                )
        )
    }
}
