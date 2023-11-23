/**
 * @(#)TypeUtils.java, 2014年3月22日.
 * <p>
 * Copyright 2014 Netease, Inc. All rights reserved.
 * NETEASE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.netease.bean2map.codec;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * @author Qiusheng
 */
public class TypeUtils {

    public static final String castToString(Object value) {
        if (value == null) {
            return null;
        }

        return value.toString();
    }

    public static final Byte castToByte(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).byteValue();
        }

        if (value instanceof String) {
            String strVal = (String) value;
            if (strVal.length() == 0) {
                return null;
            }
            return Byte.parseByte(strVal);
        }

        throw new ClassCastException("can not cast to byte, value : " + value);
    }

    public static final Character castToChar(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Character) {
            return (Character) value;
        }

        if (value instanceof String) {
            String strVal = (String) value;

            if (strVal.length() == 0) {
                return null;
            }

            if (strVal.length() != 1) {
                throw new ClassCastException("can not cast to byte, value : "
                        + value);
            }

            return strVal.charAt(0);
        }

        throw new ClassCastException("can not cast to byte, value : " + value);
    }

    public static final Short castToShort(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).shortValue();
        }

        if (value instanceof String) {
            String strVal = (String) value;
            if (strVal.length() == 0) {
                return null;
            }
            return Short.parseShort(strVal);
        }

        throw new ClassCastException("can not cast to short, value : " + value);
    }

    public static final Float castToFloat(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }

        if (value instanceof String) {
            String strVal = value.toString();
            if (strVal.length() == 0) {
                return null;
            }

            return Float.parseFloat(strVal);
        }

        throw new ClassCastException("can not cast to float, value : " + value);
    }

    public static final Double castToDouble(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            String strVal = value.toString();
            if (strVal.length() == 0) {
                return null;
            }
            return Double.parseDouble(strVal);
        }

        throw new ClassCastException("can not cast to double, value : " + value);
    }

    public static final Date castToDate(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Calendar) {
            return ((Calendar) value).getTime();
        }

        if (value instanceof Date) {
            return (Date) value;
        }

        long longValue = -1;

        if (value instanceof Number) {
            longValue = ((Number) value).longValue();
        } else if (value instanceof String) {
            String strVal = (String) value;

            if (strVal.length() == 0) {
                return null;
            }

            if (strVal.indexOf('-') != -1) {
                TimeZone timeZone = null;
                String format;
                if (strVal.length() == 10) {
                    format = "yyyy-MM-dd";
                } else if (strVal.length() == "yyyy-MM-dd HH:mm:ss".length()) {
                    format = "yyyy-MM-dd HH:mm:ss";
                } else if (strVal.length() == "yyyy-MM-ddTHH:mm:ss.SSSZ".length()) {
                    format = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
                    timeZone = TimeZone.getTimeZone("UTC");
                } else {
                    format = "yyyy-MM-dd HH:mm:ss.SSS";
                }
                SimpleDateFormat dateFormat = new SimpleDateFormat(format);
                if (timeZone != null) {
                    dateFormat.setTimeZone(timeZone);
                }

                try {
                    return dateFormat.parse(strVal);
                } catch (ParseException e) {
                    throw new ClassCastException(
                            "can not cast to Date, value : " + strVal);
                }
            }

            longValue = Long.parseLong(strVal);
        }

        if (longValue < 0) {
            throw new ClassCastException("can not cast to Date, value : "
                    + value);
        }

        return new Date(longValue);
    }

    public static final Long castToLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number) {
            return ((Number) value).longValue();
        }

        if (value instanceof String) {
            String strVal = (String) value;
            if (strVal.length() == 0) {
                return null;
            }

            try {
                return Long.parseLong(strVal);
            } catch (NumberFormatException ex) {
                //
            }
        }

        throw new ClassCastException("can not cast to long, value : " + value);
    }

    public static final Integer castToInteger(Object value) {
        return castToInt(value);
    }

    public static final Integer castToInt(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Integer) {
            return (Integer) value;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        if (value instanceof String) {
            String strVal = (String) value;
            if (strVal.length() == 0) {
                return null;
            }

            return Integer.parseInt(strVal);
        }

        throw new ClassCastException("can not cast to int, value : " + value);
    }

    public static final Boolean castToBoolean(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue() == 1;
        }

        if (value instanceof String) {
            String str = (String) value;
            if (str.length() == 0) {
                return null;
            }

            if ("true".equals(str)) {
                return Boolean.TRUE;
            }
            if ("false".equals(str)) {
                return Boolean.FALSE;
            }

            if ("1".equals(str)) {
                return Boolean.TRUE;
            }
        }

        throw new ClassCastException("can not cast to int, value : " + value);
    }

}
