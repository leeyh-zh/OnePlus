package android.support.v4.text;

import android.text.SpannableStringBuilder;
import com.google.android.gms.common.ConnectionResult;
import java.util.Locale;

public final class BidiFormatter {
    private static final int DEFAULT_FLAGS = 2;
    private static final BidiFormatter DEFAULT_LTR_INSTANCE;
    private static final BidiFormatter DEFAULT_RTL_INSTANCE;
    private static TextDirectionHeuristicCompat DEFAULT_TEXT_DIRECTION_HEURISTIC = null;
    private static final int DIR_LTR = -1;
    private static final int DIR_RTL = 1;
    private static final int DIR_UNKNOWN = 0;
    private static final String EMPTY_STRING = "";
    private static final int FLAG_STEREO_RESET = 2;
    private static final char LRE = '\u202a';
    private static final char LRM = '\u200e';
    private static final String LRM_STRING;
    private static final char PDF = '\u202c';
    private static final char RLE = '\u202b';
    private static final char RLM = '\u200f';
    private static final String RLM_STRING;
    private final TextDirectionHeuristicCompat mDefaultTextDirectionHeuristicCompat;
    private final int mFlags;
    private final boolean mIsRtlContext;

    public static final class Builder {
        private int mFlags;
        private boolean mIsRtlContext;
        private TextDirectionHeuristicCompat mTextDirectionHeuristicCompat;

        public Builder() {
            initialize(BidiFormatter.isRtlLocale(Locale.getDefault()));
        }

        public Builder(boolean rtlContext) {
            initialize(rtlContext);
        }

        public Builder(Locale locale) {
            initialize(BidiFormatter.isRtlLocale(locale));
        }

        private void initialize(boolean isRtlContext) {
            this.mIsRtlContext = isRtlContext;
            this.mTextDirectionHeuristicCompat = DEFAULT_TEXT_DIRECTION_HEURISTIC;
            this.mFlags = 2;
        }

        public android.support.v4.text.BidiFormatter.Builder stereoReset(boolean stereoReset) {
            if (stereoReset) {
                this.mFlags |= 2;
            } else {
                this.mFlags &= -3;
            }
            return this;
        }

        public android.support.v4.text.BidiFormatter.Builder setTextDirectionHeuristic(TextDirectionHeuristicCompat heuristic) {
            this.mTextDirectionHeuristicCompat = heuristic;
            return this;
        }

        private static BidiFormatter getDefaultInstanceFromContext(boolean isRtlContext) {
            return isRtlContext ? DEFAULT_RTL_INSTANCE : DEFAULT_LTR_INSTANCE;
        }

        public BidiFormatter build() {
            return (this.mFlags == 2 && this.mTextDirectionHeuristicCompat == DEFAULT_TEXT_DIRECTION_HEURISTIC) ? getDefaultInstanceFromContext(this.mIsRtlContext) : new BidiFormatter(this.mFlags, this.mTextDirectionHeuristicCompat, null);
        }
    }

    private static class DirectionalityEstimator {
        private static final byte[] DIR_TYPE_CACHE;
        private static final int DIR_TYPE_CACHE_SIZE = 1792;
        private int charIndex;
        private final boolean isHtml;
        private char lastChar;
        private final int length;
        private final CharSequence text;

        static {
            DIR_TYPE_CACHE = new byte[1792];
            for (int i = DIR_UNKNOWN; i < 1792; i++) {
                DIR_TYPE_CACHE[i] = Character.getDirectionality(i);
            }
        }

        DirectionalityEstimator(CharSequence text, boolean isHtml) {
            this.text = text;
            this.isHtml = isHtml;
            this.length = text.length();
        }

        int getEntryDir() {
            this.charIndex = 0;
            int embeddingLevel = DIR_UNKNOWN;
            int embeddingLevelDir = DIR_UNKNOWN;
            int firstNonEmptyEmbeddingLevel = DIR_UNKNOWN;
            while (this.charIndex < this.length && firstNonEmptyEmbeddingLevel == 0) {
                switch (dirTypeForward()) {
                    case DIR_UNKNOWN:
                        if (embeddingLevel == 0) {
                            return -1;
                        }
                        firstNonEmptyEmbeddingLevel = embeddingLevel;
                        break;
                    case DIR_RTL:
                    case FLAG_STEREO_RESET:
                        if (embeddingLevel == 0) {
                            return 1;
                        }
                        firstNonEmptyEmbeddingLevel = embeddingLevel;
                        break;
                    case ConnectionResult.SERVICE_INVALID:
                        break;
                    case ConnectionResult.TIMEOUT:
                    case ConnectionResult.INTERRUPTED:
                        embeddingLevel++;
                        embeddingLevelDir = DIR_LTR;
                        break;
                    case ConnectionResult.API_UNAVAILABLE:
                    case ConnectionResult.SIGN_IN_FAILED:
                        embeddingLevel++;
                        embeddingLevelDir = DIR_RTL;
                        break;
                    case ConnectionResult.SERVICE_UPDATING:
                        embeddingLevel--;
                        embeddingLevelDir = DIR_UNKNOWN;
                        break;
                    default:
                        firstNonEmptyEmbeddingLevel = embeddingLevel;
                        break;
                }
            }
            if (firstNonEmptyEmbeddingLevel == 0) {
                return 0;
            }
            if (embeddingLevelDir != 0) {
                return embeddingLevelDir;
            }
            while (this.charIndex > 0) {
                switch (dirTypeBackward()) {
                    case ConnectionResult.TIMEOUT:
                    case ConnectionResult.INTERRUPTED:
                        if (firstNonEmptyEmbeddingLevel == embeddingLevel) {
                            return -1;
                        }
                        embeddingLevel--;
                        break;
                    case ConnectionResult.API_UNAVAILABLE:
                    case ConnectionResult.SIGN_IN_FAILED:
                        if (firstNonEmptyEmbeddingLevel == embeddingLevel) {
                            return 1;
                        }
                        embeddingLevel--;
                        break;
                    case ConnectionResult.SERVICE_UPDATING:
                        embeddingLevel++;
                        break;
                    default:
                        break;
                }
            }
            return 0;
        }

        int getExitDir() {
            this.charIndex = this.length;
            int embeddingLevel = DIR_UNKNOWN;
            int lastNonEmptyEmbeddingLevel = DIR_UNKNOWN;
            while (this.charIndex > 0) {
                switch (dirTypeBackward()) {
                    case DIR_UNKNOWN:
                        if (embeddingLevel != 0 && lastNonEmptyEmbeddingLevel == 0) {
                            lastNonEmptyEmbeddingLevel = embeddingLevel;
                        }
                    case DIR_RTL:
                    case FLAG_STEREO_RESET:
                        if (embeddingLevel == 0) {
                            return 1;
                        }
                        if (lastNonEmptyEmbeddingLevel == 0) {
                            lastNonEmptyEmbeddingLevel = embeddingLevel;
                        }
                    case ConnectionResult.SERVICE_INVALID:
                        break;
                    case ConnectionResult.TIMEOUT:
                    case ConnectionResult.INTERRUPTED:
                        if (lastNonEmptyEmbeddingLevel == embeddingLevel) {
                            return DIR_LTR;
                        }
                        embeddingLevel--;
                        break;
                    case ConnectionResult.API_UNAVAILABLE:
                    case ConnectionResult.SIGN_IN_FAILED:
                        if (lastNonEmptyEmbeddingLevel == embeddingLevel) {
                            return 1;
                        }
                        embeddingLevel--;
                        break;
                    case ConnectionResult.SERVICE_UPDATING:
                        embeddingLevel++;
                        break;
                    default:
                        if (lastNonEmptyEmbeddingLevel == 0) {
                            lastNonEmptyEmbeddingLevel = embeddingLevel;
                        }
                }
            }
            return DIR_UNKNOWN;
        }

        private static byte getCachedDirectionality(char c) {
            return c < '\u0700' ? DIR_TYPE_CACHE[c] : Character.getDirectionality(c);
        }

        byte dirTypeForward() {
            this.lastChar = this.text.charAt(this.charIndex);
            if (Character.isHighSurrogate(this.lastChar)) {
                int codePoint = Character.codePointAt(this.text, this.charIndex);
                this.charIndex += Character.charCount(codePoint);
                return Character.getDirectionality(codePoint);
            }
            this.charIndex++;
            byte cachedDirectionality = getCachedDirectionality(this.lastChar);
            if (!this.isHtml) {
                return cachedDirectionality;
            }
            if (this.lastChar == '<') {
                return skipTagForward();
            }
            return this.lastChar == '&' ? skipEntityForward() : cachedDirectionality;
        }

        byte dirTypeBackward() {
            this.lastChar = this.text.charAt(this.charIndex - 1);
            if (Character.isLowSurrogate(this.lastChar)) {
                int codePoint = Character.codePointBefore(this.text, this.charIndex);
                this.charIndex -= Character.charCount(codePoint);
                return Character.getDirectionality(codePoint);
            }
            this.charIndex--;
            byte cachedDirectionality = getCachedDirectionality(this.lastChar);
            if (!this.isHtml) {
                return cachedDirectionality;
            }
            if (this.lastChar == '>') {
                return skipTagBackward();
            }
            return this.lastChar == ';' ? skipEntityBackward() : cachedDirectionality;
        }

        private byte skipTagForward() {
            int initialCharIndex = this.charIndex;
            while (this.charIndex < this.length) {
                CharSequence charSequence = this.text;
                int i = this.charIndex;
                this.charIndex = i + 1;
                this.lastChar = charSequence.charAt(i);
                if (this.lastChar == '>') {
                    return (byte) 12;
                }
                if (this.lastChar == '\"' || this.lastChar == '\'') {
                    char quote = this.lastChar;
                    while (this.charIndex < this.length) {
                        charSequence = this.text;
                        i = this.charIndex;
                        this.charIndex = i + 1;
                        char charAt = charSequence.charAt(i);
                        this.lastChar = charAt;
                        if (charAt == quote) {
                            break;
                        }
                    }
                }
            }
            this.charIndex = initialCharIndex;
            this.lastChar = '<';
            return (byte) 13;
        }

        private byte skipTagBackward() {
            int initialCharIndex = this.charIndex;
            while (this.charIndex > 0) {
                CharSequence charSequence = this.text;
                int i = this.charIndex - 1;
                this.charIndex = i;
                this.lastChar = charSequence.charAt(i);
                if (this.lastChar == '<') {
                    return (byte) 12;
                }
                if (this.lastChar == '>') {
                    break;
                } else if (this.lastChar == '\"' || this.lastChar == '\'') {
                    char quote = this.lastChar;
                    while (this.charIndex > 0) {
                        charSequence = this.text;
                        i = this.charIndex - 1;
                        this.charIndex = i;
                        char charAt = charSequence.charAt(i);
                        this.lastChar = charAt;
                        if (charAt == quote) {
                            break;
                        }
                    }
                }
            }
            this.charIndex = initialCharIndex;
            this.lastChar = '>';
            return (byte) 13;
        }

        private byte skipEntityForward() {
            while (this.charIndex < this.length) {
                CharSequence charSequence = this.text;
                int i = this.charIndex;
                this.charIndex = i + 1;
                char charAt = charSequence.charAt(i);
                this.lastChar = charAt;
                if (charAt == ';') {
                    break;
                }
            }
            return (byte) 12;
        }

        private byte skipEntityBackward() {
            int initialCharIndex = this.charIndex;
            while (this.charIndex > 0) {
                CharSequence charSequence = this.text;
                int i = this.charIndex - 1;
                this.charIndex = i;
                this.lastChar = charSequence.charAt(i);
                if (this.lastChar != '&') {
                    if (this.lastChar == ';') {
                        break;
                    }
                }
                return (byte) 12;
            }
            this.charIndex = initialCharIndex;
            this.lastChar = ';';
            return (byte) 13;
        }
    }

    static {
        DEFAULT_TEXT_DIRECTION_HEURISTIC = TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR;
        LRM_STRING = Character.toString(LRM);
        RLM_STRING = Character.toString(RLM);
        DEFAULT_LTR_INSTANCE = new BidiFormatter(false, 2, DEFAULT_TEXT_DIRECTION_HEURISTIC);
        DEFAULT_RTL_INSTANCE = new BidiFormatter(true, 2, DEFAULT_TEXT_DIRECTION_HEURISTIC);
    }

    public static BidiFormatter getInstance() {
        return new Builder().build();
    }

    public static BidiFormatter getInstance(boolean rtlContext) {
        return new Builder(rtlContext).build();
    }

    public static BidiFormatter getInstance(Locale locale) {
        return new Builder(locale).build();
    }

    private BidiFormatter(boolean isRtlContext, int flags, TextDirectionHeuristicCompat heuristic) {
        this.mIsRtlContext = isRtlContext;
        this.mFlags = flags;
        this.mDefaultTextDirectionHeuristicCompat = heuristic;
    }

    public boolean isRtlContext() {
        return this.mIsRtlContext;
    }

    public boolean getStereoReset() {
        return (this.mFlags & 2) != 0;
    }

    private String markAfter(CharSequence str, TextDirectionHeuristicCompat heuristic) {
        boolean isRtl = heuristic.isRtl(str, (int) DIR_UNKNOWN, str.length());
        if (this.mIsRtlContext || (!isRtl && getExitDir(str) != 1)) {
            return (!this.mIsRtlContext || (isRtl && getExitDir(str) != -1)) ? EMPTY_STRING : RLM_STRING;
        } else {
            return LRM_STRING;
        }
    }

    private String markBefore(CharSequence str, TextDirectionHeuristicCompat heuristic) {
        boolean isRtl = heuristic.isRtl(str, (int) DIR_UNKNOWN, str.length());
        if (this.mIsRtlContext || (!isRtl && getEntryDir(str) != 1)) {
            return (!this.mIsRtlContext || (isRtl && getEntryDir(str) != -1)) ? EMPTY_STRING : RLM_STRING;
        } else {
            return LRM_STRING;
        }
    }

    public boolean isRtl(String str) {
        return isRtl((CharSequence) str);
    }

    public boolean isRtl(CharSequence str) {
        return this.mDefaultTextDirectionHeuristicCompat.isRtl(str, (int) DIR_UNKNOWN, str.length());
    }

    public String unicodeWrap(String str, TextDirectionHeuristicCompat heuristic, boolean isolate) {
        return str == null ? null : unicodeWrap((CharSequence) str, heuristic, isolate).toString();
    }

    public CharSequence unicodeWrap(CharSequence str, TextDirectionHeuristicCompat heuristic, boolean isolate) {
        if (str == null) {
            return null;
        }
        boolean isRtl = heuristic.isRtl(str, (int) DIR_UNKNOWN, str.length());
        CharSequence result = new SpannableStringBuilder();
        if (getStereoReset() && isolate) {
            result.append(markBefore(str, isRtl ? TextDirectionHeuristicsCompat.RTL : TextDirectionHeuristicsCompat.LTR));
        }
        if (isRtl != this.mIsRtlContext) {
            result.append(isRtl ? RLE : LRE);
            result.append(str);
            result.append(PDF);
        } else {
            result.append(str);
        }
        if (!isolate) {
            return result;
        }
        TextDirectionHeuristicCompat textDirectionHeuristicCompat;
        if (isRtl) {
            textDirectionHeuristicCompat = TextDirectionHeuristicsCompat.RTL;
        } else {
            textDirectionHeuristicCompat = TextDirectionHeuristicsCompat.LTR;
        }
        result.append(markAfter(str, textDirectionHeuristicCompat));
        return result;
    }

    public String unicodeWrap(String str, TextDirectionHeuristicCompat heuristic) {
        return unicodeWrap(str, heuristic, true);
    }

    public CharSequence unicodeWrap(CharSequence str, TextDirectionHeuristicCompat heuristic) {
        return unicodeWrap(str, heuristic, true);
    }

    public String unicodeWrap(String str, boolean isolate) {
        return unicodeWrap(str, this.mDefaultTextDirectionHeuristicCompat, isolate);
    }

    public CharSequence unicodeWrap(CharSequence str, boolean isolate) {
        return unicodeWrap(str, this.mDefaultTextDirectionHeuristicCompat, isolate);
    }

    public String unicodeWrap(String str) {
        return unicodeWrap(str, this.mDefaultTextDirectionHeuristicCompat, true);
    }

    public CharSequence unicodeWrap(CharSequence str) {
        return unicodeWrap(str, this.mDefaultTextDirectionHeuristicCompat, true);
    }

    private static boolean isRtlLocale(Locale locale) {
        return TextUtilsCompat.getLayoutDirectionFromLocale(locale) == 1;
    }

    private static int getExitDir(CharSequence str) {
        return new DirectionalityEstimator(str, false).getExitDir();
    }

    private static int getEntryDir(CharSequence str) {
        return new DirectionalityEstimator(str, false).getEntryDir();
    }
}
