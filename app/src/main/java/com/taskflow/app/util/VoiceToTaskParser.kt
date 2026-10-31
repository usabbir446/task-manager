package com.taskflow.app.util

import com.taskflow.app.data.local.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

object VoiceToTaskParser {

    private val highPriorityEN = listOf("urgent", "important", "critical", "asap", "high priority", "immediately")
    private val lowPriorityEN = listOf("low priority", "not urgent", "whenever", "no rush")
    private val mediumPriorityEN = listOf("medium priority", "moderate", "normal priority")

    private val highPriorityBN = listOf("জরুরি", "গুরুত্বপূর্ণ", "এখনই", "দরকার", "তাড়াতাড়ি", "আজকেই")
    private val lowPriorityBN = listOf("পরে", "কম গুরুত্ব", "তাড়া নেই", "সময় পেলে")
    private val mediumPriorityBN = listOf("মাঝারি", "সাধারণ")

    private val categoryKeywordsEN = mapOf(
        "Work" to listOf("work", "office", "meeting", "project", "client", "report", "email", "presentation", "deadline", "standup"),
        "Personal" to listOf("personal", "home", "family", "self", "health", "doctor", "gym", "exercise"),
        "Shopping" to listOf("buy", "purchase", "shop", "shopping", "grocery", "groceries", "order"),
        "Study" to listOf("study", "learn", "read", "course", "class", "homework", "assignment", "exam"),
        "Finance" to listOf("pay", "bill", "payment", "bank", "money", "salary", "invest", "budget")
    )

    private val categoryKeywordsBN = mapOf(
        "Work" to listOf("কাজ", "অফিস", "মিটিং", "প্রজেক্ট", "ক্লায়েন্ট", "রিপোর্ট", "ইমেইল"),
        "Personal" to listOf("ব্যক্তিগত", "বাসা", "পরিবার", "স্বাস্থ্য", "ডাক্তার", "ব্যায়াম"),
        "Shopping" to listOf("কেনা", "কিনতে", "বাজার", "শপিং", "অর্ডার"),
        "Study" to listOf("পড়া", "পড়াশোনা", "শেখা", "কোর্স", "ক্লাস", "পরীক্ষা"),
        "Finance" to listOf("টাকা", "বিল", "পেমেন্ট", "ব্যাংক", "বেতন", "বিনিয়োগ")
    )

    private val numberWordsEN = mapOf(
        "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
        "fifteen" to 15, "twenty" to 20, "thirty" to 30, "forty" to 40,
        "forty five" to 45, "fifty" to 50
    )

    private val numberWordsBN = mapOf(
        "এক" to 1, "১" to 1, "১লা" to 1, "পহেলা" to 1, "প্রথম" to 1,
        "দুই" to 2, "২" to 2, "২রা" to 2, "দোশরা" to 2,
        "তিন" to 3, "৩" to 3, "৩রা" to 3, "তেসরা" to 3,
        "চার" to 4, "৪" to 4, "৪ঠা" to 4, "চৌঠা" to 4,
        "পাঁচ" to 5, "পাচ" to 5, "৫" to 5, "৫ই" to 5, "পাঁচই" to 5,
        "ছয়" to 6, "ছয়" to 6, "৬" to 6, "৬ই" to 6, "ছয়ই" to 6,
        "সাত" to 7, "৭" to 7, "৭ই" to 7, "সাতই" to 7,
        "আট" to 8, "৮" to 8, "৮ই" to 8, "আটই" to 8,
        "নয়" to 9, "নয়" to 9, "৯" to 9, "৯ই" to 9, "নয়ই" to 9,
        "দশ" to 10, "১০" to 10, "১০ই" to 10, "দশই" to 10,
        "এগারো" to 11, "১১" to 11, "১১ই" to 11,
        "বারো" to 12, "১২" to 12, "১২ই" to 12,
        "তেরো" to 13, "১৩" to 13, "১৩ই" to 13,
        "চৌদ্দ" to 14, "১৪" to 14, "১৪ই" to 14,
        "পনের" to 15, "পনেরো" to 15, "১৫" to 15, "১৫ই" to 15,
        "ষোলো" to 16, "১৬" to 16, "১৬ই" to 16,
        "সতেরো" to 17, "১৭" to 17, "১৭ই" to 17,
        "আঠারো" to 18, "১৮" to 18, "১৮ই" to 18,
        "উনিশ" to 19, "১৯" to 19, "১৯ই" to 19,
        "বিশ" to 20, "কুড়ি" to 20, "২০" to 20, "২০ই" to 20,
        "একুশ" to 21, "২১" to 21, "২১ই" to 21,
        "বাইশ" to 22, "২২" to 22, "২২ই" to 22,
        "তেইশ" to 23, "২৩" to 23, "২৩ই" to 23,
        "চব্বিশ" to 24, "২৪" to 24, "২৪ই" to 24,
        "পঁচিশ" to 25, "২৫" to 25, "২৫ই" to 25,
        "ছাব্বিশ" to 26, "২৬" to 26, "২৬ই" to 26,
        "সাতাশ" to 27, "২৭" to 27, "২৭ই" to 27,
        "আঠাশ" to 28, "২৮" to 28, "২৮ই" to 28,
        "উনত্রিশ" to 29, "২৯" to 29, "২৯ই" to 29,
        "ত্রিশ" to 30, "৩০" to 30, "৩০ই" to 30,
        "একত্রিশ" to 31, "৩১" to 31, "৩১ই" to 31
    )

    private val bengaliMonths = mapOf(
        "জানুয়ারি" to 0, "জানুয়ারি" to 0, "january" to 0, "jan" to 0,
        "ফেব্রুয়ারি" to 1, "ফেব্রুয়ারি" to 1, "february" to 1, "feb" to 1,
        "মার্চ" to 2, "march" to 2, "mar" to 2,
        "এপ্রিল" to 3, "april" to 3, "apr" to 3,
        "মে" to 4, "may" to 4,
        "জুন" to 5, "june" to 5, "jun" to 5,
        "জুলাই" to 6, "july" to 6, "jul" to 6,
        "আগস্ট" to 7, "অগাস্ট" to 7, "august" to 7, "aug" to 7,
        "সেপ্টেম্বর" to 8, "september" to 8, "sep" to 8, "sept" to 8,
        "অক্টোবর" to 9, "october" to 9, "oct" to 9,
        "নভেম্বর" to 10, "november" to 10, "nov" to 10,
        "ডিসেম্বর" to 11, "december" to 11, "dec" to 11
    )

    private val bengaliWeekdays = mapOf(
        "রবিবার" to Calendar.SUNDAY, "রবি" to Calendar.SUNDAY, "sunday" to Calendar.SUNDAY, "sun" to Calendar.SUNDAY,
        "সোমবার" to Calendar.MONDAY, "সোম" to Calendar.MONDAY, "monday" to Calendar.MONDAY, "mon" to Calendar.MONDAY,
        "মঙ্গলবার" to Calendar.TUESDAY, "মঙ্গল" to Calendar.TUESDAY, "tuesday" to Calendar.TUESDAY, "tue" to Calendar.TUESDAY,
        "বুধবার" to Calendar.WEDNESDAY, "বুধ" to Calendar.WEDNESDAY, "wednesday" to Calendar.WEDNESDAY, "wed" to Calendar.WEDNESDAY,
        "বৃহস্পতিবার" to Calendar.THURSDAY, "বৃহস্পতি" to Calendar.THURSDAY, "thursday" to Calendar.THURSDAY, "thu" to Calendar.THURSDAY,
        "শুক্রবার" to Calendar.FRIDAY, "শুক্র" to Calendar.FRIDAY, "friday" to Calendar.FRIDAY, "fri" to Calendar.FRIDAY,
        "শনিবার" to Calendar.SATURDAY, "শনি" to Calendar.SATURDAY, "saturday" to Calendar.SATURDAY, "sat" to Calendar.SATURDAY
    )

    fun parseVoiceInput(text: String, language: String = "en"): TaskEntity {
        val trimmedText = text.trim()
        val lowerText = trimmedText.lowercase()
        
        val priority = detectPriority(lowerText, language)
        val category = detectCategory(lowerText, language)
        val dueDate = extractDueDate(lowerText)
        val recurrence = detectRecurrence(lowerText)
        val (cleanedText, subtasksList) = extractSubTasks(trimmedText)
        val (rawTitle, extractedDesc) = extractTitleAndDescription(cleanedText)
        val finalTitle = cleanTaskTitle(rawTitle)

        val subtasksString = subtasksList.joinToString(",") { "$it:false" }

        // Ensure date is clearly visible in description
        val dateDesc = if (dueDate != null) {
            val sdf = SimpleDateFormat("EEE, MMM dd • hh:mm a", Locale.getDefault())
            "📅 Scheduled: ${sdf.format(Date(dueDate))}"
        } else ""

        val finalDescription = when {
            extractedDesc.isNotBlank() && dateDesc.isNotBlank() -> "$dateDesc\n$extractedDesc"
            extractedDesc.isNotBlank() -> extractedDesc
            else -> dateDesc
        }

        return TaskEntity(
            title = if (finalTitle.isNotBlank()) finalTitle else rawTitle,
            description = finalDescription,
            priority = priority,
            category = category,
            dueDate = dueDate,
            recurrence = recurrence,
            subTasks = subtasksString,
            isFromVoice = true,
            voiceLanguage = language
        )
    }

    private fun detectPriority(text: String, language: String): Int {
        val highKeywords = if (language == "bn") highPriorityBN + highPriorityEN else highPriorityEN + highPriorityBN
        val lowKeywords = if (language == "bn") lowPriorityBN + lowPriorityEN else lowPriorityEN + lowPriorityBN
        val mediumKeywords = if (language == "bn") mediumPriorityBN + mediumPriorityEN else mediumPriorityEN + mediumPriorityBN

        return when {
            highKeywords.any { text.contains(it) } -> 3
            lowKeywords.any { text.contains(it) } -> 1
            mediumKeywords.any { text.contains(it) } -> 2
            else -> 2
        }
    }

    private fun detectCategory(text: String, language: String): String {
        val allKeywords = if (language == "bn") categoryKeywordsBN + categoryKeywordsEN else categoryKeywordsEN + categoryKeywordsBN
        for ((category, keywords) in allKeywords) {
            if (keywords.any { text.contains(it) }) return category
        }
        return "General"
    }

    private fun detectRecurrence(text: String): String {
        return when {
            text.contains("everyday") || text.contains("daily") || text.contains("প্রতিদিন") || text.contains("রোজ") -> "Daily"
            text.contains("every weekday") || text.contains("weekdays") || text.contains("কর্মদিবস") -> "Weekdays"
            text.contains("weekly") || text.contains("every week") || text.contains("প্রতি সপ্তাহে") -> "Weekly"
            else -> "None"
        }
    }

    fun extractDueDate(text: String): Long? {
        val calendar = Calendar.getInstance()
        var dateSpecified = false
        var timeSpecified = false

        // 1. Relative Minutes (e.g. "after 5 min", "in 10 mins", "৫ মিনিট পর")
        val relMinutesPattern = Pattern.compile(
            "(?:remind\\s+me\\s+)?(?:in|after)?\\s*([০-৯\\d]+|[a-zA-Z\\u0980-\\u09FF]+)\\s*(?:minutes?|mins?|min|মিনিট|মিন)\\s*(?:later|after|পর|পরে)?",
            Pattern.CASE_INSENSITIVE
        )
        val relMinMatcher = relMinutesPattern.matcher(text)
        if (relMinMatcher.find()) {
            val numStr = relMinMatcher.group(1)?.trim() ?: "5"
            val mins = parseNumber(numStr) ?: 5
            return System.currentTimeMillis() + (mins * 60 * 1000L)
        }

        // 2. Relative Hours (e.g. "after 2 hours", "in 1 hr", "২ ঘণ্টা পর")
        val relHoursPattern = Pattern.compile(
            "(?:remind\\s+me\\s+)?(?:in|after)?\\s*([০-৯\\d]+|[a-zA-Z\\u0980-\\u09FF]+)\\s*(?:hours?|hrs?|hr|ঘণ্টা|ঘন্টার|ঘন্টা)\\s*(?:later|after|পর|পরে)?",
            Pattern.CASE_INSENSITIVE
        )
        val relHoursMatcher = relHoursPattern.matcher(text)
        if (relHoursMatcher.find()) {
            val numStr = relHoursMatcher.group(1)?.trim() ?: "1"
            val hrs = parseNumber(numStr) ?: 1
            return System.currentTimeMillis() + (hrs * 3600 * 1000L)
        }

        // 3. Explicit Month + Day (e.g. "১ নভেম্বর", "1st November", "নভেম্বর ১", "১৫ তারিখ")
        val monthPatternStr = bengaliMonths.keys.map { Regex.escape(it) + "(?:ের|র|এর)?" }.joinToString("|")
        val numKeys = numberWordsBN.keys.sortedByDescending { it.length }.map { Regex.escape(it) }.joinToString("|")

        // Pattern A: Day then Month (e.g. "১ নভেম্বর", "1st November", "১৫ই অক্টোবর", "১০ nov", "পহেলা নভেম্বর")
        val dayMonthRegex = Regex("([০-৯\\d]{1,2}|$numKeys)\\s*(?:লা|রা|ঠা|ই|th|st|nd|rd)?\\s*(?:তারিখ|তারিখে)?\\s*($monthPatternStr)", RegexOption.IGNORE_CASE)
        val dmMatch = dayMonthRegex.find(text)
        if (dmMatch != null) {
            val dayToken = dmMatch.groupValues[1]
            val monthToken = cleanMonthToken(dmMatch.groupValues[2])
            val dayVal = parseNumber(dayToken)
            val monthVal = bengaliMonths[monthToken]
            if (dayVal != null && monthVal != null && dayVal in 1..31) {
                calendar.set(Calendar.MONTH, monthVal)
                calendar.set(Calendar.DAY_OF_MONTH, dayVal)
                dateSpecified = true
            }
        }

        // Pattern B: Month then Day (e.g. "নভেম্বর ১", "November 1", "নভেম্বর মাসের ৫ তারিখ", "Nov 15")
        if (!dateSpecified) {
            val monthDayRegex = Regex("($monthPatternStr)\\s*(?:মাসের|এর|র)?\\s*([০-৯\\d]{1,2}|$numKeys)\\s*(?:তারিখ|তারিখে|লা|রা|ঠা|ই|th|st|nd|rd)?", RegexOption.IGNORE_CASE)
            val mdMatch = monthDayRegex.find(text)
            if (mdMatch != null) {
                val monthToken = cleanMonthToken(mdMatch.groupValues[1])
                val dayToken = mdMatch.groupValues[2]
                val monthVal = bengaliMonths[monthToken]
                val dayVal = parseNumber(dayToken)
                if (monthVal != null && dayVal != null && dayVal in 1..31) {
                    calendar.set(Calendar.MONTH, monthVal)
                    calendar.set(Calendar.DAY_OF_MONTH, dayVal)
                    dateSpecified = true
                }
            }
        }

        // Pattern C: Just Day + তারিখ (e.g. "৫ তারিখ", "১০ তারিখে", "15 tarikh")
        if (!dateSpecified) {
            val tarikhRegex = Regex("([০-৯\\d]{1,2}|$numKeys)\\s*(?:লা|রা|ঠা|ই)?\\s*(?:তারিখ|তারিখে|tarikh)", RegexOption.IGNORE_CASE)
            val tMatch = tarikhRegex.find(text)
            if (tMatch != null) {
                val dayToken = tMatch.groupValues[1]
                val dayVal = parseNumber(dayToken)
                if (dayVal != null && dayVal in 1..31) {
                    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
                    if (dayVal < currentDay) {
                        // Specified day has passed this month, schedule for next month
                        calendar.add(Calendar.MONTH, 1)
                    }
                    calendar.set(Calendar.DAY_OF_MONTH, dayVal)
                    dateSpecified = true
                }
            }
        }

        // Pattern D: Weekday (e.g. "শুক্রবার", "আগামী সোমবার", "Friday")
        if (!dateSpecified) {
            for ((wName, wDay) in bengaliWeekdays) {
                if (Regex("(?<![\\p{L}])$wName(?![\\p{L}])", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                    val currentWDay = calendar.get(Calendar.DAY_OF_WEEK)
                    var daysToAdd = wDay - currentWDay
                    if (daysToAdd <= 0) daysToAdd += 7
                    calendar.add(Calendar.DAY_OF_YEAR, daysToAdd)
                    dateSpecified = true
                    break
                }
            }
        }

        // 4. Relative Day: "আজ", "কাল", "পরশু"
        // CRITICAL: NEVER match "কাল" inside "সকাল" (morning) or "বিকাল" (afternoon) or "গতকাল" (yesterday)!
        if (!dateSpecified) {
            if (Regex("(?<![\\p{L}])(?:আজকে|আজকের|আজ|today)(?![\\p{L}])", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                dateSpecified = true
            } else if (Regex("(?<![সবিগত\\p{L}])(?:আগামী\\s*কাল(?:কে)?|কালকে|কালকের|কাল|tomorrow)(?![\\p{L}])", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                dateSpecified = true
            } else if (Regex("(?<![\\p{L}])(?:পরশুদিন|পরশুর|পরশু|day after tomorrow)(?![\\p{L}])", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                calendar.add(Calendar.DAY_OF_YEAR, 2)
                dateSpecified = true
            } else if (Regex("(?<![\\p{L}])(?:তরশুদিন|তরশু)(?![\\p{L}])", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                calendar.add(Calendar.DAY_OF_YEAR, 3)
                dateSpecified = true
            }
        }

        // 5. Military/4-digit Time Format: e.g. "0930 hr", "0930 hrs", "0930", "1430 hr", "at 0930"
        val militaryPattern = Pattern.compile("(?:at|by)?\\s*([0-2]\\d)([0-5]\\d)\\s*(?:hrs?|hours?)?", Pattern.CASE_INSENSITIVE)
        val milMatcher = militaryPattern.matcher(text)
        if (milMatcher.find()) {
            val hour = milMatcher.group(1)?.toIntOrNull() ?: 9
            val minute = milMatcher.group(2)?.toIntOrNull() ?: 0
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            timeSpecified = true
        }

        // 6. Bengali Special Spoken Times: দেড়টা (1:30), আড়াইটা (2:30), সাড়ে [X]টা (half-past)
        if (!timeSpecified) {
            if (Regex("(?:দেড়টা|দেরটা)").containsMatchIn(text)) {
                val hour = if (text.contains("রাত")) 1 else 13
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, 30)
                calendar.set(Calendar.SECOND, 0)
                timeSpecified = true
            } else if (Regex("(?:আড়াইটা|আরাইটা)").containsMatchIn(text)) {
                val hour = if (text.contains("রাত")) 2 else 14
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, 30)
                calendar.set(Calendar.SECOND, 0)
                timeSpecified = true
            }
        }

        // Bangla "সাড়ে [X]টা" (e.g. "সাড়ে ৯টা", "সকাল সাড়ে নয়টা")
        if (!timeSpecified) {
            val shareRegex = Regex("(সকাল|দুপুর|বিকেল|সন্ধ্যা|রাত)?\\s*সাড়ে\\s*([০-৯\\d]{1,2}|$numKeys)\\s*(?:টা|টায়|টায়)?")
            val sMatch = shareRegex.find(text)
            if (sMatch != null) {
                val period = sMatch.groupValues[1]
                val rawHour = sMatch.groupValues[2]
                val parsedHour = parseNumber(rawHour)
                if (parsedHour != null && parsedHour in 1..24) {
                    var hour = parsedHour
                    if ((period.contains("বিকেল") || period.contains("সন্ধ্যা") || period.contains("রাত")) && hour < 12) hour += 12
                    else if (period.contains("দুপুর") && hour in 1..4) hour += 12

                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, 30)
                    calendar.set(Calendar.SECOND, 0)
                    timeSpecified = true
                }
            }
        }

        // 7. General Bengali Time Formats: e.g. "সকাল ৯টা ৩০", "সকাল ১০টায়", "বিকেল ৩টা", "রাত ৯টা"
        if (!timeSpecified) {
            val bnPattern = Pattern.compile("(সকাল|দুপুর|বিকেল|সন্ধ্যা|রাত)?\\s*([০-৯\\d]{1,2})(?::|টা|টায়|টায়)?\\s*([০-৯\\d]{1,2})?\\s*(?:মিনিট|মিন)?(?:টা|টায়|টায়)?")
            val bnMatcher = bnPattern.matcher(text)
            if (bnMatcher.find()) {
                val period = bnMatcher.group(1) ?: ""
                val rawHour = bnMatcher.group(2)
                val rawMinute = bnMatcher.group(3)

                if (!rawHour.isNullOrEmpty()) {
                    val parsedHour = convertBengaliToEnglishDigits(rawHour).toIntOrNull()
                    if (parsedHour != null && parsedHour in 1..24) {
                        var hour = parsedHour
                        val minute = if (!rawMinute.isNullOrEmpty()) {
                            convertBengaliToEnglishDigits(rawMinute).toIntOrNull() ?: 0
                        } else 0

                        if ((period.contains("বিকেল") || period.contains("সন্ধ্যা") || period.contains("রাত")) && hour < 12) {
                            hour += 12
                        } else if (period.contains("দুপুর") && hour in 1..4) {
                            hour += 12
                        }

                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)
                        timeSpecified = true
                    }
                }
            }
        }

        // 8. English Time Formats with explicit Colon, Spoken minutes, or AM/PM
        if (!timeSpecified) {
            val enTimePattern = Pattern.compile(
                "(?:at|by)?\\s*(\\d{1,2})(?::|\\s+)?(\\d{2})?\\s*(?:in\\s+the\\s+morning|in\\s+the\\s+afternoon|in\\s+the\\s+evening|at\\s+night|am|pm|hrs?|hours?)?",
                Pattern.CASE_INSENSITIVE
            )
            val enMatcher = enTimePattern.matcher(text)
            while (enMatcher.find()) {
                val hourStr = enMatcher.group(1)
                val minStr = enMatcher.group(2)
                val matched = enMatcher.group(0)?.lowercase() ?: ""

                val parsedHour = hourStr?.toIntOrNull()
                val isRelevant = parsedHour != null && (
                    minStr != null ||
                    matched.contains("am") || matched.contains("pm") ||
                    matched.contains("morning") || matched.contains("afternoon") ||
                    matched.contains("evening") || matched.contains("night") ||
                    matched.contains("hr") || matched.contains("at ") || matched.contains("by ")
                )

                if (parsedHour != null && isRelevant) {
                    var hour = parsedHour
                    val minute = minStr?.toIntOrNull() ?: 0

                    if ((text.contains("pm") || text.contains("afternoon") || text.contains("evening") || text.contains("night")) && hour < 12) {
                        hour += 12
                    }
                    if ((text.contains("am") || text.contains("in the morning") || text.contains("morning")) && hour == 12) {
                        hour = 0
                    }

                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    timeSpecified = true
                    break
                }
            }
        }

        // 9. Spoken English numbers: "nine thirty in the morning", "ten fifteen am"
        if (!timeSpecified) {
            val spokenPattern = Pattern.compile(
                "(one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve)\\s+(fifteen|twenty|thirty|forty|forty\\s+five|fifty)?\\s*(?:in\\s+the\\s+morning|in\\s+the\\s+afternoon|in\\s+the\\s+evening|am|pm)?",
                Pattern.CASE_INSENSITIVE
            )
            val spokenMatcher = spokenPattern.matcher(text)
            if (spokenMatcher.find()) {
                val hourWord = spokenMatcher.group(1)?.lowercase() ?: ""
                val minWord = spokenMatcher.group(2)?.lowercase()?.replace(" ", "") ?: ""

                val hourVal = numberWordsEN[hourWord] ?: 9
                val minVal = when (minWord) {
                    "fifteen" -> 15
                    "twenty" -> 20
                    "thirty" -> 30
                    "forty" -> 40
                    "fortyfive" -> 45
                    "fifty" -> 50
                    else -> 0
                }

                var hour = hourVal
                if ((text.contains("pm") || text.contains("afternoon") || text.contains("evening") || text.contains("night")) && hour < 12) {
                    hour += 12
                }
                if ((text.contains("am") || text.contains("in the morning") || text.contains("morning")) && hour == 12) {
                    hour = 0
                }

                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minVal)
                calendar.set(Calendar.SECOND, 0)
                timeSpecified = true
            }
        }

        // Final Time Resolution
        if (timeSpecified) {
            // Only bump day to tomorrow if NO specific date was requested AND the time today has already passed!
            if (!dateSpecified && calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }

        if (dateSpecified) {
            // If date was specified but no time was mentioned, default to 9:30 AM on that specified date
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 30)
            calendar.set(Calendar.SECOND, 0)
            return calendar.timeInMillis
        }

        return null
    }

    private fun cleanMonthToken(token: String): String {
        return token.lowercase().replace("ের", "").replace("এর", "").replace("র", "").trim()
    }

    private fun parseNumber(token: String): Int? {
        val cleanToken = token.lowercase().replace("লা", "").replace("রা", "").replace("ঠা", "").replace("ই", "").replace("th", "").replace("st", "").replace("nd", "").replace("rd", "").trim()
        val directInt = convertBengaliToEnglishDigits(cleanToken).toIntOrNull()
        if (directInt != null) return directInt
        if (numberWordsEN.containsKey(cleanToken)) return numberWordsEN[cleanToken]
        if (numberWordsBN.containsKey(cleanToken)) return numberWordsBN[cleanToken]
        return null
    }

    private fun cleanTaskTitle(rawTitle: String): String {
        var cleaned = rawTitle
            .replace(Regex("(?i)^(?:remind\\s+me\\s+to|remind\\s+me\\s+about|remind\\s+me\\s+)\\s*"), "")
            .replace(Regex("(?i)\\b(?:in|after)\\s+\\d+\\s*(?:minutes?|mins?|min|hours?|hrs?|hr)\\b"), "")
            .replace(Regex("(?i)\\b\\d+\\s*(?:minutes?|mins?|min|hours?|hrs?|hr)\\s*(?:later|after)\\b"), "")
            .replace(Regex("[০-৯\\d]+\\s*(?:মিনিট|মিন|ঘণ্টা|ঘন্টার)\\s*(?:পর|পরে)"), "")
            .replace(Regex("(?i)\\b(?:at|by)?\\s*\\d{1,4}\\s*(?:am|pm|hrs?|hours?|in\\s+the\\s+morning)?\\b"), "")
            .replace(Regex("(?i)^(?:to|for)\\s+"), "")
            .trim()

        if (cleaned.isBlank()) cleaned = rawTitle
        return cleaned.replaceFirstChar { it.uppercase() }
    }

    private fun convertBengaliToEnglishDigits(input: String): String {
        val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        var result = input
        for (i in bnDigits.indices) {
            result = result.replace(bnDigits[i], (i + 48).toChar())
        }
        return result
    }

    private fun extractSubTasks(text: String): Pair<String, List<String>> {
        val colonIndex = text.indexOf(':')
        if (colonIndex in 1..(text.length - 2)) {
            val titlePart = text.substring(0, colonIndex).trim()
            val subtasksPart = text.substring(colonIndex + 1).trim()
            val items = subtasksPart.split(',', ';', '।', '\n').map { it.trim() }.filter { it.isNotEmpty() }
            if (items.size > 1) {
                return Pair(titlePart, items)
            }
        }
        return Pair(text, emptyList())
    }

    private fun extractTitleAndDescription(text: String): Pair<String, String> {
        val separators = listOf(". ", "। ", ", details ", ", বিস্তারিত ", " - ", " and then ", " তারপর ")
        for (separator in separators) {
            val index = text.indexOf(separator, ignoreCase = true)
            if (index > 0 && index < text.length - separator.length) {
                val title = text.substring(0, index).trim()
                val description = text.substring(index + separator.length).trim()
                if (title.isNotEmpty() && description.isNotEmpty()) {
                    return Pair(title.replaceFirstChar { it.uppercase() }, description.replaceFirstChar { it.uppercase() })
                }
            }
        }
        if (text.length > 60) {
            val cutoff = text.indexOf(' ', 40)
            if (cutoff > 0) {
                return Pair(text.substring(0, cutoff).trim().replaceFirstChar { it.uppercase() }, text.substring(cutoff).trim().replaceFirstChar { it.uppercase() })
            }
        }
        return Pair(text.replaceFirstChar { it.uppercase() }, "")
    }
}
