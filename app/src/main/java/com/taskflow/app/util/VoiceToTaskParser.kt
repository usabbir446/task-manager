package com.taskflow.app.util

import com.taskflow.app.data.local.TaskEntity
import java.util.Calendar
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
        "এক" to 1, "দুই" to 2, "তিন" to 3, "চার" to 4, "পাঁচ" to 5, "পাচ" to 5,
        "ছয়" to 6, "ছয়" to 6, "সাত" to 7, "আট" to 8, "নয়" to 9, "নয়" to 9,
        "দশ" to 10, "পনের" to 15, "পনেরো" to 15, "কুড়ি" to 20, "বিশ" to 20,
        "পঁচিশ" to 25, "ত্রিশ" to 30, "পঁয়ত্রিশ" to 35, "চল্লিশ" to 40, "পঞ্চাশ" to 50
    )

    fun parseVoiceInput(text: String, language: String = "en"): TaskEntity {
        val trimmedText = text.trim()
        val lowerText = trimmedText.lowercase()
        
        val priority = detectPriority(lowerText, language)
        val category = detectCategory(lowerText, language)
        val dueDate = extractDueDate(lowerText)
        val recurrence = detectRecurrence(lowerText)
        val (cleanedText, subtasksList) = extractSubTasks(trimmedText)
        val (rawTitle, description) = extractTitleAndDescription(cleanedText)
        val finalTitle = cleanTaskTitle(rawTitle)

        val subtasksString = subtasksList.joinToString(",") { "$it:false" }

        return TaskEntity(
            title = if (finalTitle.isNotBlank()) finalTitle else rawTitle,
            description = description,
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

        // 1. Relative Minutes (e.g. "after 5 min", "in 5 minutes", "remind me after 5 min", "5 mins later", "৫ মিনিট পর")
        val relMinutesPattern = Pattern.compile(
            "(?:remind\\s+me\\s+)?(?:in|after)?\\s*([০-৯\\d]+|[a-zA-Z\\u0980-\\u09FF]+)\\s*(?:minutes?|mins?|min|মিনিট|মিনিটের|মিন)\\s*(?:later|after|পর|পরে)?",
            Pattern.CASE_INSENSITIVE
        )
        val relMinsMatcher = relMinutesPattern.matcher(text)
        if (relMinsMatcher.find()) {
            val numStr = relMinsMatcher.group(1)?.trim() ?: "5"
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

        // 3. Detect relative day offset
        if (text.contains("tomorrow") || text.contains("কালকে") || text.contains("আগামীকাল") || text.contains("কাল")) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            dateSpecified = true
        } else if (text.contains("day after tomorrow") || text.contains("পরশু") || text.contains("পরশুদিন")) {
            calendar.add(Calendar.DAY_OF_YEAR, 2)
            dateSpecified = true
        } else if (text.contains("today") || text.contains("আজকে") || text.contains("আজ")) {
            dateSpecified = true
        }

        // 4. Bengali Time Formats: e.g. "সকাল ১০টায়", "বিকেল ৩টা", "রাত ৯টা", "দুপুর ২:১৫"
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

        // 5. English Time Formats: e.g. "at 3 PM", "at 10:30 am", "at 5:00", "by 4 pm"
        if (!timeSpecified) {
            val enTimePattern = Pattern.compile("(?:at|by)?\\s*(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
            val enMatcher = enTimePattern.matcher(text)
            while (enMatcher.find()) {
                val hourStr = enMatcher.group(1)
                val minStr = enMatcher.group(2)
                val amPm = enMatcher.group(3)?.lowercase()

                val parsedHour = hourStr?.toIntOrNull()
                if (parsedHour != null && (amPm != null || text.contains("at ") || text.contains("by "))) {
                    var hour = parsedHour
                    val minute = minStr?.toIntOrNull() ?: 0
                    if (amPm == "pm" && hour < 12) hour += 12
                    if (amPm == "am" && hour == 12) hour = 0

                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    timeSpecified = true
                    break
                }
            }
        }

        if (dateSpecified && !timeSpecified) {
            calendar.set(Calendar.HOUR_OF_DAY, 9)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            return calendar.timeInMillis
        }

        if (timeSpecified) {
            if (!dateSpecified && calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
            return calendar.timeInMillis
        }

        return null
    }

    private fun parseNumber(token: String): Int? {
        val cleanToken = token.lowercase().trim()
        val directInt = convertBengaliToEnglishDigits(cleanToken).toIntOrNull()
        if (directInt != null) return directInt
        if (numberWordsEN.containsKey(cleanToken)) return numberWordsEN[cleanToken]
        if (numberWordsBN.containsKey(cleanToken)) return numberWordsBN[cleanToken]
        return null
    }

    private fun cleanTaskTitle(rawTitle: String): String {
        // Remove trailing or leading reminder prefixes like "remind me after 5 min", "in 10 mins", "after 5 min to ..."
        var cleaned = rawTitle
            .replace(Regex("(?i)^(?:remind\\s+me\\s+to|remind\\s+me\\s+about|remind\\s+me\\s+)\\s*"), "")
            .replace(Regex("(?i)\\b(?:in|after)\\s+\\d+\\s*(?:minutes?|mins?|min|hours?|hrs?|hr)\\b"), "")
            .replace(Regex("(?i)\\b\\d+\\s*(?:minutes?|mins?|min|hours?|hrs?|hr)\\s*(?:later|after)\\b"), "")
            .replace(Regex("[০-৯\\d]+\\s*(?:মিনিট|মিন|ঘণ্টা|ঘন্টার)\\s*(?:পর|পরে)"), "")
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
