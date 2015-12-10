package com.example.recursiontest;

import android.text.TextUtils;

/**
 * This class is for contiguous versions expression validation, such as (10000, 12030], which are composed of
 * several components:
 * <pre>
 *    starting tag
 *    |
 *    |  lower version code
 *    |  |
 *    |  |  separator
 *    |  |  |
 *    |  |  | allowable blanks
 *    |  |  | |
 *    |  |  | |  higher version code
 *    |  |  | |  |
 *    |  |  | |  |  ending tag
 *    |  |  | |  |  |
 *    (10000,  12030]
 * </pre>
 *          
 *  several constraints should be obeyed on the version expression:
 *  <ol> 
 *	 <li>MUST have a starting tag and an ending tag. here ‘(’ is starting tag while ']' is an end;
 *   <li>Starting/ending tag can be either inclusive or exclusive. here, ‘[‘ or ‘]’ means inclusive, while ‘(‘ or ‘)’ means exclusive;</li>
 *	 <li>Higher version code should be greater or at least equal to lower version code;</li>
 *	 <li>Starting version code and ending version code is separated by a separator, here ',' is the separator. Furthermore, several consecutive 
 *       blanks('' or 't') are allowed right after the separator and before the higher version code. </li>
 *   <li>Version code characters must be digits. means only 0-9 are allowed for version code characters;</li>
 *   <li>Either lower version code or higher version code can be empty to support unlimited lower limit and higher limit separately.</li>
 *  </ol>
 *
 *  According to the constraints listed above,  version expression could possibly be:
 *  <ul>
 *   <li>[1000, 1000]    // legal, exactly version 1000 </li>
 *   <li>[1000, 1000)    // legal, exactly version 1000 </li>
 *   <li>(1000, 1000]    // legal,  exactly version 1000</li>
 *   <li>(1000, 1000)    // illegal version code, ending version code should greater than starting version code</li>
 *   <li>[1000, 1203]    // legal,  from version code 1000 (including) to 1203 (including)</li>
 *   <li>[1000, 1203)    // legal,  from version code 1000 (including) to 1203 (excluding)</li>
 *   <li>(1000, 1203]    // legal,  from version code 1000 (excluding) to 1203 (including)</li>
 *   <li>(1000, 1203)    // legal,  from version code 1000 (excluding) to 1203 (excluding)</li>
 *   <li>(1203, 1000)    // illegal version code, ending version code should greater than starting version code</li>
 *   <li>(1000a, 1203)   // illegal version code character ‘a’, only digits 0-9 are allowed.</li>
 *   <li>[1000,]         // legal version code, greater or equal to 1000.</li>
 *   <li>[1000,)         // legal version code, greater or equal to 1000.</li>
 *   <li>(, 1000)        // legal version code, lower than 1000, excluding.</li>
 *   <li>[, 1000]        // legal version code, lower than 1000, including.</li>
 *   <li>(,)             // legal version code, support all versions.</li>
 *   
 *   <p>
 *   <b>Note:</b> the starting tag may not necessarily be '[' or '(', likely, the ending tag may not necessarily be ']' or ')', 
 *   cause they are not JSON friendly.
 *   <p>
 */
public class VersionExpressionValidator {

    public static final int STARTING_TAG_INCLUSIVE = '[';
    public static final int STARTING_TAG_EXCLUSIVE = '(';
    public static final int ENDING_TAG_INCLUSIVE = ']';
    public static final int ENDING_TAG_EXCLUSIVE = ')';

    private int mLowerVersionCode = Integer.MIN_VALUE;

    private int mHigherVersionCode = Integer.MAX_VALUE;
    
    private int mStartingTag = STARTING_TAG_INCLUSIVE;
    
    private int mEndingTag = ENDING_TAG_EXCLUSIVE;

    private Separator mSeparator;

    public VersionExpressionValidator(String expression) throws VersionCheckException {
        parse(expression);
    }

    private void parse(String expression) throws VersionCheckException {

        if (expression == null || expression.length() == 0) {
            throw new VersionCheckException("empty version expression.");
        }

        char ch = expression.charAt(0);
        if (!isValidStartingTag(ch)) {
            throw new VersionCheckException("invalid starting character " + ch);
        }
        mStartingTag = ch;

        ch = expression.charAt(expression.length() - 1);
        if (!isValidEndingTag(ch)) {
            throw new VersionCheckException("invalid ending character " + ch);
        }
        mEndingTag = ch;

        // work on a new copy of the expression.
        final String exp = new String(expression);
        final int size = exp.length();

        // skip starting tag.
        int start = 1;
        int end = 1;
        String subStr = "";

        for (int pos = 1; pos < size;) {
            ch = exp.charAt(pos++);
            if (Character.isDigit(ch)) {
                end++;
            } else if (Separator.isValidSeparator(ch)) {
                mSeparator = Separator.valueOf(ch);
                subStr = exp.substring(start, end);
                try {
                    mLowerVersionCode = Integer.valueOf(subStr);
                    end = start = pos;
                } catch (NumberFormatException e) {
                    if (TextUtils.isEmpty(subStr)) {
                     // empty string are allowed here to support unlimited lower version, such as (, 1000]
                    } else {
                        throw new VersionCheckException("Failed to convert " + subStr + " to an integer, " + "cause " + ch
                                + " is an illegal version number.");
                    }
                }
            } else if (isBlankCharacter(ch)) {
                // skip black characters.
                end = start = pos;
                continue;
            } else if (isValidEndingTag(ch)) {
                subStr = exp.substring(start, end);
                try {
                    mHigherVersionCode = Integer.valueOf(subStr);
                } catch (NumberFormatException e) {
                    if (TextUtils.isEmpty(subStr)) {
                        // empty string are allowed here to support unlimited higher version, such as [1000,)
                    } else {
                        throw new VersionCheckException("Failed to convert " + subStr + " to an integer.");
                    }
                }
            } else {
                throw new VersionCheckException(ch + " is an illegal version number.");
            }

        }

        if (mLowerVersionCode > 0 && mStartingTag == STARTING_TAG_EXCLUSIVE) {
            mLowerVersionCode--;
        }
        
        if (mHigherVersionCode > 0 && mEndingTag == ENDING_TAG_EXCLUSIVE) {
            mHigherVersionCode--;
        }
        
        if (mLowerVersionCode > mHigherVersionCode) {
            throw new VersionCheckException("lower version code " + mLowerVersionCode
                    + " is greater than higher version code " + mHigherVersionCode);
        }
        
    }

    private boolean isValidStartingTag(int tag) {
        return tag == STARTING_TAG_INCLUSIVE || tag == STARTING_TAG_EXCLUSIVE;
    }

    private boolean isValidEndingTag(int tag) {
        return tag == ENDING_TAG_INCLUSIVE || tag == ENDING_TAG_EXCLUSIVE;
    }

    private boolean isBlankCharacter(int ch) {
        return ch == ' ' || ch == 't';
    }

    public int getLowerVersionCode() {
        return mLowerVersionCode;
    }

    public int getHigherVersionCode() {
        return mHigherVersionCode;
    }

    public Separator getSeparator() {
        return mSeparator;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.valueOf((char)mStartingTag))
          .append(mLowerVersionCode)
          .append(mSeparator)
          .append(mHigherVersionCode)
          .append(String.valueOf((char)mEndingTag))
          .append("\n");
        return sb.toString();
    }

    public static enum Separator {

        COMMA(','), 
        SEMICOLON(';');

        private int value;

        Separator(int val) {
            this.value = val;
        }

        public static final boolean isValidSeparator(int separator) {
            boolean isValid = false;

            for (Separator s : values()) {

                if (s.value == separator) {
                    isValid = true;
                    break;
                }
            }

            return isValid;
        }

        public static final Separator valueOf(int ch) {

            for (Separator s : values()) {

                if (s.value == ch) {
                    return s;
                }
            }

            return null;
        }
        
        public int value() {
            return value;
        }

        @Override
        public String toString() {
            return String.valueOf((char)value);
        }
    }

    public static class VersionCheckException extends Exception {

        private static final long serialVersionUID = 1L;

        public VersionCheckException() {
            super();
        }

        public VersionCheckException(String message) {
            super(message);
        }

    }

}
