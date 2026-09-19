<div align="center">
  <img src="logo.svg" width="72" alt="">
  <h1>MuteAds</h1>
  <p><strong>Mute the ads, not the app.</strong></p>
  <p>
    An Android notification filter that reads each notification as it arrives
    and clears only the ones trying to sell you something.
  </p>
</div>

---

## The problem

Your bank texts you a login code. It also texts you a loan offer. One switch controls
both, so you leave it on and take the ads with the code.

That is the whole shape of it. The apps that spam hardest are the ones you cannot
afford to silence: the bank, the courier, the airline. Most of them give marketing no
separate channel, and the few that do bury it four screens into settings.

Android already hands every notification to any app you trust with notification
access. So the fix does not have to be per app. It can be per notification.

## How it works

When a notification arrives, MuteAds turns it into plain text and asks
[Jev](https://typesafe.ai) three typed questions. Jev is a decision model: instead of
writing sentences it returns calibrated probabilities, which come back in about four
hundred milliseconds.

```
is_promo    Is this selling, nudging, or pulling you back?
is_needed   Is there a code, a delivery, an appointment, a person inside?
category    promo · transactional · personal · system · content
```

A notification is cleared only when **both** gates agree:

```kotlin
val kill = verdict.promo >= threshold && verdict.needed <= 0.35
```

One question would be too blunt. A bank's loan campaign is marketing, and the same
bank sends your login code. Two independent gates tell them apart.

## Measured behaviour

Real notifications, scored by Jev, at the default threshold of 90%:

| Notification | is_promo | is_needed | Result |
| --- | ---: | ---: | --- |
| "Final hours, 70% off everything" | 0.99 | 0.07 | cleared |
| "Personal loan offer, approved in two minutes" | 0.97 | 0.07 | cleared |
| "You are about to lose your streak" | 0.96 | 0.03 | cleared |
| "New season, strong looks" | 0.97 | 0.05 | cleared |
| "Your code is 481923" | 0.02 | 0.95 | kept |
| "Parcel #4417 arrives before 6pm" | 0.02 | 0.91 | kept |
| "Are you coming to dinner? Around 8." | 0.02 | 0.76 | kept |
| "Design review in 10 minutes" | 0.03 | 0.73 | kept |
| "New sign-in on Windows" | 0.02 | 0.74 | kept |

`is_needed` comes back bimodal: promotional notifications land between 0.03 and 0.12,
anything you actually want lands between 0.69 and 0.95. The 0.35 ceiling sits in the
empty band between them rather than hugging either edge.

If you edit the rubric, measure this table again. Shortening it twice cost real
accuracy: dropping the phrase about unrequested product offers took a bank loan
campaign from 0.95 down to 0.68, and framing the question purely around commerce took
a streak reminder from 0.96 to 0.68, because a streak reminder sells nothing.

## What it never touches

Filtered out on the device, before any request leaves your phone:

- Phone calls, alarms, navigation, media transport, progress and system categories
- Anything ongoing or tied to a foreground service
- Anything you cannot swipe away yourself
- The dialer, telecom, clock, settings and System UI packages

And if the API is slow, unreachable, rejects the key, or returns something it cannot
parse, the notification is left exactly where it was. Doubt is never a reason to
delete. Those cases are logged as **Skipped** so you can see them.

## Setup

**1. Install the app.** Grab the APK from
[Releases](https://github.com/emre-aktas/muteads/releases), or build it yourself:

```bash
./gradlew assembleDebug
```

There are no third-party dependencies. No AndroidX, no OkHttp, no JSON library.
`HttpURLConnection` and `org.json` come with the framework, so the whole build is one
Gradle task and the APK is under a megabyte.

**2. Get a Jev key** from [console.typesafe.ai](https://console.typesafe.ai).

**3. Open Settings in the app**, paste the key, and grant notification access. It
starts judging on the next notification that arrives.

The key is stored in `SharedPreferences` on your device and sent to nobody but the
Jev API. You are billed for your own usage, which at a hundred notifications a day
works out to roughly a dollar a year.

## What it costs

A notification is about 560 input tokens. At Jev's $0.042 per million input tokens,
with output free, that is $0.000024 each.

| | A year at 100 notifications a day |
| --- | --- |
| Jev | $0.86 |
| A small LLM at $0.20/M | $4.10 |
| A frontier LLM at $10/M | $205 |

Billing for the question instead of an essay is what makes it affordable to ask about
every notification rather than maintaining a keyword list by hand.

## Changing the rules

The three questions live in [`Rubric.kt`](app/src/main/java/dev/emreaktas/muteads/Rubric.kt),
written in plain English. Edit them, rebuild, and the filter behaves differently. No
retraining, no dataset, no model to fine-tune. The confidence threshold is a slider in
the app.

The rubric is re-sent with every notification, so every extra sentence is paid for on
each call in tokens and upload time. Keep it short, but measure after you cut.

## Project layout

```
app/src/main/java/dev/emreaktas/muteads/
  NotificationFilterService.kt   the listener, the guard rails, the dedupe
  JevClient.kt                   one HTTPS call, fails open
  Rubric.kt                      the three questions
  Store.kt                       preferences and the decision log
  MainActivity.kt                Activity / Insights / Settings
  Ui.kt                          design tokens and view builders
```

`Ui.kt` holds a small design system: a mutable token object for light and dark, plus
builders for cards, badges, buttons, the switch, the slider and the charts. Everything
is built in code, so there are no layout XML files to keep in sync.

## Things worth knowing

**Your phone may still make a sound.** Android plays the alert the moment a
notification is posted and only hands it to apps like this one afterwards. A promo can
buzz, then disappear from the shade a few hundred milliseconds later. Whether it makes
any sound at all depends on the channel's importance and your Do Not Disturb settings.

**Nothing disappears silently.** Every decision is written to a rolling log of the
last 150, with the probabilities behind it. A filter you cannot audit is a filter you
will not trust.

**Apps that stream text into a live notification** post it several times. The filter
hashes the content, so one notification costs one call, and the log keeps one row
rather than five.

**The toolchain is deliberately old.** AGP 7.0.4, Gradle 7.2, JDK 15, compileSdk 29.
It was built on a machine without JDK 17, and the zero-dependency design is what made
that possible. It will build on anything newer too.

## Limitations

- Android only. iOS has no equivalent to `NotificationListenerService`: third-party
  apps there can only touch their own notifications, so this cannot be ported.
- Calibration is an aggregate property. Across many predictions, answers given 90%
  are right about 90% of the time. That says nothing about any single answer.
- Jev is young and has one provider. Measure it against your own notifications before
  you rely on it.

## Licence

MIT. See [LICENSE](LICENSE).

---

Built by [Emre Aktaş](https://x.com/emredsgn).
