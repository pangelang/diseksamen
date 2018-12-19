package utils;

public final class Encryption {

  public static String encryptDecryptXOR(String rawString) {

    // If encryption is enabled in Config.
    if (Config.getEncryption()) {

      // The key is predefined and hidden in code
      // TODO: Create a more complex code and store it somewhere better: FIX (See config)

      // Stringbuilder enables you to play around with strings and make useful stuff
      StringBuilder thisIsEncrypted = new StringBuilder();

      // TODO: This is where the magic of XOR is happening. Are you able to explain what is going on?: FIX
      /**
       * For loop with 'i' as counting variable is initiated. It runs as long as 'i' is smaller than the length of the
       * rawString variable which is a String of the json data. This is the String that is parsed from our endpoints
       * to be encrypted. The value of thisIsEncrypted object is updated using StringBuilder .append taking char as
       * parameter. Every char at index 'i' is then subjet to the ^ (XOR) operation which takes the binary value of the
       * char and a certain char in our encryption key defined in the config class and changes the values. The char from
       * our key is found by using the modulo operation, which takes the index 'i' and divides it with the length of the
       * key and finds the remainding value.
       *
       * Example of XOR operation with rawString = "a" and encryption key = "b":
       * Binary value of "a" is = 0110 0001 and "b" = 0110 0010.
       * XOR operation on these = 0000 0011 which is = "3" when converted back to ASCII text output.
       */
      for (int i = 0; i < rawString.length(); i++) {
        thisIsEncrypted.append((char) (rawString.charAt(i) ^ Config.getKey()[i % Config.getKey().length]));
      }

      // We return the encrypted string
      return thisIsEncrypted.toString();

    } else {
      // We return without having done anything
      return rawString;
    }
  }
}
