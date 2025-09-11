// Transliteration mapping for common Bangla to English phonetic conversions
const banglaToEnglishMap: Record<string, string> = {
  // Vowels
  'অ': 'a', 'আ': 'aa', 'ই': 'i', 'ঈ': 'ii', 'উ': 'u', 'ঊ': 'uu',
  'ঋ': 'ri', 'এ': 'e', 'ঐ': 'oi', 'ও': 'o', 'ঔ': 'ou',
  
  // Consonants
  'ক': 'k', 'খ': 'kh', 'গ': 'g', 'ঘ': 'gh', 'ঙ': 'ng',
  'চ': 'ch', 'ছ': 'chh', 'জ': 'j', 'ঝ': 'jh', 'ঞ': 'ny',
  'ট': 't', 'ঠ': 'th', 'ড': 'd', 'ঢ': 'dh', 'ণ': 'n',
  'ত': 't', 'থ': 'th', 'দ': 'd', 'ধ': 'dh', 'ন': 'n',
  'প': 'p', 'ফ': 'ph', 'ব': 'b', 'ভ': 'bh', 'ম': 'm',
  'য': 'y', 'র': 'r', 'ল': 'l', 'শ': 'sh', 'ষ': 'sh',
  'স': 's', 'হ': 'h', 'ড়': 'r', 'ঢ়': 'rh', 'য়': 'y',
  'ৎ': 't', 'ং': 'ng', 'ঃ': 'h', 'ঁ': 'n',
  
  // Vowel marks (kar)
  'া': 'aa', 'ি': 'i', 'ী': 'ii', 'ু': 'u', 'ূ': 'uu',
  'ৃ': 'ri', 'ে': 'e', 'ৈ': 'oi', 'ো': 'o', 'ৌ': 'ou',
  
  // Numbers
  '০': '0', '১': '1', '২': '2', '৩': '3', '৪': '4',
  '৫': '5', '৬': '6', '৭': '7', '৮': '8', '৯': '9',
};

// Common English to Bangla phonetic mappings
const englishToBanglaMap: Record<string, string[]> = {
  'dhaka': ['ঢাকা', 'ধাকা'],
  'chittagong': ['চট্টগ্রাম', 'চিটাগং'],
  'sylhet': ['সিলেট'],
  'rajshahi': ['রাজশাহী'],
  'khulna': ['খুলনা'],
  'barisal': ['বরিশাল', 'বরিসাল'],
  'rangpur': ['রংপুর'],
  'mymensingh': ['ময়মনসিংহ'],
  'comilla': ['কুমিল্লা'],
  'cox': ['কক্স'],
  'bazar': ['বাজার'],
  'bangladesh': ['বাংলাদেশ'],
  'bengal': ['বাংলা', 'বেঙ্গল'],
  'hotel': ['হোটেল'],
  'restaurant': ['রেস্তোরাঁ', 'রেস্টুরেন্ট'],
  'transport': ['পরিবহন', 'ট্রান্সপোর্ট'],
  'bus': ['বাস'],
  'train': ['ট্রেন'],
  'flight': ['ফ্লাইট'],
  'tour': ['ট্যুর', 'ভ্রমণ'],
  'travel': ['ভ্রমণ', 'ট্রাভেল'],
  'booking': ['বুকিং'],
  'package': ['প্যাকেজ'],
};

/**
 * Normalize Bangla text to English phonetic equivalent
 */
export function banglaToEnglish(text: string): string {
  let result = '';
  
  for (const char of text) {
    if (banglaToEnglishMap[char]) {
      result += banglaToEnglishMap[char];
    } else if (char.match(/[a-zA-Z0-9\s]/)) {
      result += char.toLowerCase();
    }
  }
  
  return result.trim();
}

/**
 * Generate possible Bangla variations for English text
 */
export function englishToBangla(text: string): string[] {
  const lowerText = text.toLowerCase().trim();
  const variations: string[] = [];
  
  // Check for exact matches
  if (englishToBanglaMap[lowerText]) {
    variations.push(...englishToBanglaMap[lowerText]);
  }
  
  // Check for partial matches
  Object.keys(englishToBanglaMap).forEach(key => {
    if (lowerText.includes(key) || key.includes(lowerText)) {
      variations.push(...englishToBanglaMap[key]);
    }
  });
  
  return [...new Set(variations)];
}

/**
 * Remove diacritics and normalize text for search
 */
export function normalizeForSearch(text: string): string {
  return text
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '') // Remove diacritics
    .replace(/[^\w\s]/g, '') // Remove special characters
    .trim();
}

/**
 * Generate search variations for both Bangla and English input
 */
export function generateSearchVariations(query: string): {
  original: string;
  normalized: string;
  transliterated: string[];
  language: 'bn' | 'en' | 'mixed';
} {
  const trimmedQuery = query.trim();
  
  // Detect language
  const hasBangla = /[\u0980-\u09FF]/.test(trimmedQuery);
  const hasEnglish = /[a-zA-Z]/.test(trimmedQuery);
  
  let language: 'bn' | 'en' | 'mixed' = 'en';
  if (hasBangla && hasEnglish) {
    language = 'mixed';
  } else if (hasBangla) {
    language = 'bn';
  }
  
  const normalized = normalizeForSearch(trimmedQuery);
  const transliterated: string[] = [];
  
  if (language === 'bn' || language === 'mixed') {
    // Convert Bangla to English
    const englishVersion = banglaToEnglish(trimmedQuery);
    if (englishVersion && englishVersion !== normalized) {
      transliterated.push(englishVersion);
    }
  }
  
  if (language === 'en' || language === 'mixed') {
    // Convert English to Bangla variations
    const banglaVariations = englishToBangla(trimmedQuery);
    transliterated.push(...banglaVariations);
  }
  
  return {
    original: trimmedQuery,
    normalized,
    transliterated: [...new Set(transliterated)],
    language,
  };
}

/**
 * Enhanced search function that includes transliteration
 */
export function enhancedSearch(query: string): {
  searchTerms: string[];
  languageHint: string;
} {
  const variations = generateSearchVariations(query);
  
  const searchTerms = [
    variations.original,
    variations.normalized,
    ...variations.transliterated,
  ].filter(term => term && term.length > 0);
  
  return {
    searchTerms: [...new Set(searchTerms)],
    languageHint: variations.language,
  };
}

/**
 * Check if text contains Bangla characters
 */
export function isBanglaText(text: string): boolean {
  return /[\u0980-\u09FF]/.test(text);
}

/**
 * Check if text contains English characters
 */
export function isEnglishText(text: string): boolean {
  return /[a-zA-Z]/.test(text);
}