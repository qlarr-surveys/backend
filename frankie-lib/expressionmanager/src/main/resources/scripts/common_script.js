FrankieScripts = {
    separator: function(lang) {
        if (lang == "ar") {
            return "، "
        } else {
            return ", "
        }
    },
    and: function(lang) {
        if (lang == "ar") {
            return " و "
        } else {
            return " & "
        }
    },
    isValidSqlDateTime: function(sqlDateTime) {
        if (typeof sqlDateTime !== "string" || !/^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$/.test(sqlDateTime)) {
            return false
        }
        var t = sqlDateTime.split(/[- :]/);
        if (!FrankieScripts.isValidDay(parseInt(t[0]), parseInt(t[1]), parseInt(t[2]))) {
            return false
        }
        if (!FrankieScripts.isValidTime(sqlDateTime.split(/[ ]/)[1])) {
            return false
        }
        var date = new Date(t[0], t[1] - 1, t[2], t[3], t[4], t[5]);
        return !isNaN(date)
    },
    listStrings: function(arr, lang) {
        if (arr.length == 0) {
            return "";
        } else if (arr.length == 1) {
            return arr[0];
        }
        return arr.slice(0, -1).join(FrankieScripts.separator(lang)) + FrankieScripts.and(lang) + arr[arr.length - 1];
    },
    isValidTime: function(time) {
        if (typeof time !== "string" || !/^(\d{2}):(\d{2}):(\d{2})$/.test(time)) {
            return false
        }
        var t = time.split(/[:]/);
        var hours = parseInt(t[0]);
        var minutes = parseInt(t[1]);
        var seconds = parseInt(t[2]);
        return hours >= 0 && hours <= 23 && minutes >= 0 && minutes <= 59 &&
            seconds >= 0 && seconds <= 59;
    },
    isValidDay: function(year, month, day) {
        switch (month) {
            case 2:
                return (year % 4 == 0) ? (day <= 29 && day >= 1) : (day <= 28 && day >= 1);
            case 9:
            case 4:
            case 6:
            case 11:
                return day <= 30 && day >= 1;
            case 1:
            case 3:
            case 4:
            case 7:
            case 8:
            case 10:
            case 12:
                return day <= 31 && day >= 1;
            default:
                return false
        }
    },
    sqlDateTimeToDate: function(sqlDateTime) {
        if (typeof sqlDateTime !== "string" || !/^(\d{4})-(\d{2})-(\d{2}) (\d{2}):(\d{2}):(\d{2})$/.test(sqlDateTime)) {
            sqlDateTime = "0000-01-01 00:00:00"
        }
        var t = sqlDateTime.split(/[- :]/);
        var date = new Date(t[0], t[1] - 1, t[2], t[3], t[4], t[5]);
        if (isNaN(date)) {
            return new Date(0000, 0, 1, 0, 0, 0);
        }
        return date;
    },
    formatSqlDate: function(sqlDateTime, format) {
        if (!FrankieScripts.isValidSqlDateTime(sqlDateTime)) {
            return ""
        }
        var t = sqlDateTime.split(/[- :]/);
        return format.replace("YYYY", t[0]).replace("MM", t[1]).replace("DD", t[2])
    },
    formatTime: function(sqlDateTime, fullDayFormat) {
        if (!FrankieScripts.isValidSqlDateTime(sqlDateTime)) {
            return ""
        }
        var t = sqlDateTime.split(/[- :]/);
        if(fullDayFormat){
            return t[3]+":"+t[4]
        } else {
            var hourInt = parseInt(t[3])
            var symbol = (hourInt > 11) ? "PM" : "AM"
            var hourValue = (hourInt % 12 == 0) ? 12 : hourInt % 12
            return ('00' + hourValue).slice(-2)+":"+t[4] + " " + symbol
        }
    },
    dateStringToDate: function(sqlDate) {
        if (typeof sqlDate !== "string" || !/^(\d{4})-(\d{2})-(\d{2})$/.test(sqlDate)) {
            sqlDate = "0000-01-01"
        }
        var t = sqlDate.split(/-/);
        var date = new Date(t[0], t[1] - 1, t[2], 0, 0, 0);
        if (isNaN(date)) {
            return new Date(0000, 0, 1, 0, 0, 0);
        }
        return date;
    },
    toSqlDateTimeIgnoreTime: function(date) {
        return date.getFullYear() + '-' +
            ('00' + (date.getMonth() + 1)).slice(-2) + '-' +
            ('00' + date.getDate()).slice(-2) +
            ' 00:00:00';
    },
    toSqlDateTime: function(date) {
        return date.getFullYear() + '-' +
            ('00' + (date.getMonth() + 1)).slice(-2) + '-' +
            ('00' + date.getDate()).slice(-2) + ' ' +
            ('00' + date.getHours()).slice(-2) + ':' +
            ('00' + date.getMinutes()).slice(-2) + ':' +
            ('00' + date.getSeconds()).slice(-2);
    },
    toSqlDateTimeIgnoreDate: function(time) {
        return "1970-01-01 " +
            ('00' + time.getHours()).slice(-2) + ':' +
            ('00' + time.getMinutes()).slice(-2) + ':' +
            ('00' + time.getSeconds()).slice(-2);
    },
    isVoid: function(value) {
        if (value === undefined || value === null || value === "") {
            return true;
        } else {
            return false;
        }
    },
    isNotVoid: function(value) {
        return !FrankieScripts.isVoid(value);
    },
    wordCount: function(value) {
        if (!value) {
            return 0;
        } else {
            return value.split(/\s+/).filter(function(word) {
                return word
            }).length
        }
    },
    hasDuplicates: function(arr) {
        var seen = {};
        for (var i = 0; i < arr.length; i++) {
            var item = arr[i];
            if (seen[item]) {
                return true;
            }
            seen[item] = true;
        }
        return false;
    }
}