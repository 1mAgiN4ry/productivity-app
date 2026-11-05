package com.example.productivity.data

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class Converters {
    @TypeConverter fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter fun dateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()
    @TypeConverter fun epochDayToDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter fun timeToMinuteOfDay(value: LocalTime?): Int? =
        value?.let { it.hour * 60 + it.minute }
    @TypeConverter fun minuteOfDayToTime(value: Int?): LocalTime? =
        value?.let { LocalTime.of(it / 60, it % 60) }
}
