import { useTranslation } from 'react-i18next';

/**
 * Bangla to English transliteration mapping
 */
const BANGLA_TO_ENGLISH_MAP: Record<string, string> = {
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
  
  // Vowel marks
  'া': 'aa', 'ি': 'i', 'ী': 'ii', 'ু': 'u', 'ূ': 'uu',
  'ৃ': 'ri', 'ে': 'e', 'ৈ': 'oi', 'ো': 'o', 'ৌ': 'ou',
  
  // Numbers
  '০': '0', '১': '1', '২': '2', '৩': '3', '৪': '4',
  '৫': '5', '৬': '6', '৭': '7', '৮': '8', '৯': '9'
};

/**
 * English to Bangla transliteration mapping (reverse)
 */
const ENGLISH_TO_BANGLA_MAP: Record<string, string> = {
  // Common English to Bangla mappings
  'dhaka': 'ঢাকা',
  'chittagong': 'চট্টগ্রাম',
  'sylhet': 'সিলেট',
  'rajshahi': 'রাজশাহী',
  'khulna': 'খুলনা',
  'barisal': 'বরিশাল',
  'rangpur': 'রংপুর',
  'mymensingh': 'ময়মনসিংহ',
  'cox': 'কক্স',
  'bazar': 'বাজার',
  'bangladesh': 'বাংলাদেশ',
  'bangla': 'বাংলা',
  'hotel': 'হোটেল',
  'restaurant': 'রেস্তোরাঁ',
  'beach': 'সমুদ্র সৈকত',
  'hill': 'পাহাড়',
  'river': 'নদী',
  'lake': 'হ্রদ',
  'park': 'পার্ক',
  'museum': 'জাদুঘর',
  'mosque': 'মসজিদ',
  'temple': 'মন্দির',
  'church': 'গির্জা'
};

/**
 * Normalize text by removing diacritics and converting to lowercase
 */
export function normalizeText(text: string): string {
  return text
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '') // Remove diacritics
    .trim();
}

/**
 * Transliterate Bangla text to English
 */
export function banglaToEnglish(text: string): string {
  let result = '';
  for (const char of text) {
    result += BANGLA_TO_ENGLISH_MAP[char] || char;
  }
  return normalizeText(result);
}

/**
 * Transliterate English text to Bangla (basic mapping)
 */
export function englishToBangla(text: string): string {
  const normalized = normalizeText(text);
  
  // Check for exact matches first
  if (ENGLISH_TO_BANGLA_MAP[normalized]) {
    return ENGLISH_TO_BANGLA_MAP[normalized];
  }
  
  // Check for partial matches
  for (const [english, bangla] of Object.entries(ENGLISH_TO_BANGLA_MAP)) {
    if (normalized.includes(english)) {
      return bangla;
    }
  }
  
  return text; // Return original if no mapping found
}

/**
 * Generate search variations for a query
 */
export function generateSearchVariations(query: string): string[] {
  const variations = new Set<string>();
  const normalized = normalizeText(query);
  
  // Add original query
  variations.add(query);
  variations.add(normalized);
  
  // Add transliterations
  const banglaVariation = englishToBangla(normalized);
  const englishVariation = banglaToEnglish(query);
  
  variations.add(banglaVariation);
  variations.add(englishVariation);
  
  // Add word-by-word transliterations
  const words = normalized.split(/\s+/);
  if (words.length > 1) {
    const transliteratedWords = words.map(word => {
      const bangla = englishToBangla(word);
      const english = banglaToEnglish(word);
      return [word, bangla, english];
    }).flat();
    
    variations.add(transliteratedWords.join(' '));
  }
  
  return Array.from(variations).filter(v => v.trim().length > 0);
}

/**
 * Check if text contains Bangla characters
 */
export function containsBangla(text: string): boolean {
  return /[\u0980-\u09FF]/.test(text);
}

/**
 * Check if text contains English characters
 */
export function containsEnglish(text: string): boolean {
  return /[a-zA-Z]/.test(text);
}

/**
 * Smart search function that handles both Bangla and English queries
 */
export function createSmartSearchQuery(query: string): {
  original: string;
  normalized: string;
  variations: string[];
  isBangla: boolean;
  isEnglish: boolean;
} {
  const normalized = normalizeText(query);
  const variations = generateSearchVariations(query);
  
  return {
    original: query,
    normalized,
    variations,
    isBangla: containsBangla(query),
    isEnglish: containsEnglish(query)
  };
}

/**
 * React hook for transliteration search
 */
export function useTransliterationSearch() {
  const { i18n } = useTranslation();
  
  const searchWithTransliteration = async (
    query: string,
    searchFunction: (searchTerms: string[]) => Promise<any[]>
  ) => {
    const searchQuery = createSmartSearchQuery(query);
    
    // Use AI service for better transliteration if available
    let enhancedVariations = searchQuery.variations;
    
    try {
      // Call AI service for enhanced transliteration
      const aiResponse = await fetch('/api/v1/ai/transliterate', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept-Language': i18n.language
        },
        body: JSON.stringify({
          text: query,
          sourceLanguage: searchQuery.isBangla ? 'bn' : 'en',
          targetLanguage: searchQuery.isBangla ? 'en' : 'bn'
        })
      });
      
      if (aiResponse.ok) {
        const aiResult = await aiResponse.json();
        if (aiResult.transliteration) {
          enhancedVariations = [...enhancedVariations, aiResult.transliteration];
        }
      }
    } catch (error) {
      console.warn('AI transliteration service unavailable, using fallback:', error);
    }
    
    // Remove duplicates and empty strings
    const uniqueVariations = Array.from(new Set(enhancedVariations))
      .filter(v => v.trim().length > 0);
    
    return searchFunction(uniqueVariations);
  };
  
  return {
    searchWithTransliteration,
    createSmartSearchQuery,
    banglaToEnglish,
    englishToBangla,
    normalizeText
  };
}

/**
 * Highlight search terms in text with transliteration support
 */
export function highlightSearchTerms(
  text: string,
  searchQuery: string,
  highlightClass: string = 'bg-yellow-200'
): string {
  const searchVariations = generateSearchVariations(searchQuery);
  let highlightedText = text;
  
  searchVariations.forEach(variation => {
    if (variation.trim().length > 0) {
      const regex = new RegExp(`(${variation.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
      highlightedText = highlightedText.replace(
        regex,
        `<span class="${highlightClass}">$1</span>`
      );
    }
  });
  
  return highlightedText;
}