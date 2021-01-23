package smite;

// TODO: add encryption scheme
// TODO: refactor so that unit flags and location flags
// can better share common elements.

public class Flag {

    int flag; // Must always be between 0 and 2^FLAG_BITS - 1
    int writtenTo;
    public final static int FLAG_BITS = 24;
    public final static int SCHEMA_BITS = 3;

    /*
     * TODO: if we start running out of schema space, some of these schemas can
     * be made to have the same value because they can only come from a specific
     * unit type (e.g. SPAWN_UNIT_SCHEMA can only come from ECs, while MAP_TERRAIN_SCHEMA
     * can only come from scouts, so it is safe to have them be the same value.)
     */
    public static final int NO_SCHEMA = 0; // all units have flag zero by default
	public static final int EC_SIGHTING_SCHEMA = 1;
    public static final int MAP_INFO_SCHEMA = 2;
    public static final int LOCATION_SCHEMA = 3;
    public static final int SPAWN_UNIT_SCHEMA = 4;
	public static final int SPAWN_DESTINATION_SCHEMA = 5;
	public static final int UNIT_UPDATE_SCHEMA = 6;
    public static final int MIDGAME_ALLY_SCHEMA = 7;

    public Flag() {
        flag = 0;
        writtenTo = 32 - FLAG_BITS; // 8-bit offset to make the first 8 bits blank
    }

    /**
     * constructor for reading/parsing a flag
     */
    public Flag(int received) {
        flag = received;
        writtenTo = 32;
	}

    public int getFlag() {
        return flag;
    }

    /**
     * Get the schema directly from a raw flag value.
     *
     * Note: if we add encryption later, we'll need to decrypt here.
     */
    public static int getSchema(int flag) {
        int bitm = bitmask2(32 - FLAG_BITS, 32 - FLAG_BITS + SCHEMA_BITS, true);
        return (flag & bitm) >>> (FLAG_BITS - SCHEMA_BITS);
    }

    public int getSchema() {
        return readFromFlag(0, SCHEMA_BITS);
    }

    public boolean setSchema(int schema) {
        return writeToFlag(schema, SCHEMA_BITS);
    }

    /**
     * Allocate the next numBits bits in the flag's contents to write in value.
     * It is up to the caller to provide enough bits to write the value. Otherwise,
     * the function will not work. It will only write the first numBits bits.
     */
    public boolean writeToFlag(int value, int numBits) {
        if (numBits + writtenTo > 32) {
            return false;
        }
        int bitm = bitmask2(writtenTo, writtenTo + numBits, false);
        value = value << (32 - writtenTo - numBits);
        flag &= bitm;
        flag |= value;
        writtenTo += numBits;
        return true;
    }

    /**
     * Read numBits bits from the flag, starting at startBit, which must be
     * between 0 and FLAG_BITS - 1 inclusive.
     */
    public int readFromFlag(int startBit, int numBits) {
        startBit += 32 - FLAG_BITS; // 8-bit offset to account for blank first 8 bits
        int bitm = bitmask2(startBit, startBit + numBits, true);
        return (flag & bitm) >>> (32 - startBit - numBits);
    }

    public static int bitmask2(int start, int end, boolean read) {
        int num = start*33+end;
        switch (num) {
			case 0: if(read) { return 0; } else { return -1; }
			case 1: if(read) { return -2147483648; } else { return 2147483647; }
			case 2: if(read) { return -1073741824; } else { return 1073741823; }
			case 3: if(read) { return -536870912; } else { return 536870911; }
			case 4: if(read) { return -268435456; } else { return 268435455; }
			case 5: if(read) { return -134217728; } else { return 134217727; }
			case 6: if(read) { return -67108864; } else { return 67108863; }
			case 7: if(read) { return -33554432; } else { return 33554431; }
			case 8: if(read) { return -16777216; } else { return 16777215; }
			case 9: if(read) { return -8388608; } else { return 8388607; }
			case 10: if(read) { return -4194304; } else { return 4194303; }
			case 11: if(read) { return -2097152; } else { return 2097151; }
			case 12: if(read) { return -1048576; } else { return 1048575; }
			case 13: if(read) { return -524288; } else { return 524287; }
			case 14: if(read) { return -262144; } else { return 262143; }
			case 15: if(read) { return -131072; } else { return 131071; }
			case 16: if(read) { return -65536; } else { return 65535; }
			case 17: if(read) { return -32768; } else { return 32767; }
			case 18: if(read) { return -16384; } else { return 16383; }
			case 19: if(read) { return -8192; } else { return 8191; }
			case 20: if(read) { return -4096; } else { return 4095; }
			case 21: if(read) { return -2048; } else { return 2047; }
			case 22: if(read) { return -1024; } else { return 1023; }
			case 23: if(read) { return -512; } else { return 511; }
			case 24: if(read) { return -256; } else { return 255; }
			case 25: if(read) { return -128; } else { return 127; }
			case 26: if(read) { return -64; } else { return 63; }
			case 27: if(read) { return -32; } else { return 31; }
			case 28: if(read) { return -16; } else { return 15; }
			case 29: if(read) { return -8; } else { return 7; }
			case 30: if(read) { return -4; } else { return 3; }
			case 31: if(read) { return -2; } else { return 1; }
			case 32: if(read) { return -1; } else { return 0; }
			case 34: if(read) { return 0; } else { return -1; }
			case 35: if(read) { return 1073741824; } else { return -1073741825; }
			case 36: if(read) { return 1610612736; } else { return -1610612737; }
			case 37: if(read) { return 1879048192; } else { return -1879048193; }
			case 38: if(read) { return 2013265920; } else { return -2013265921; }
			case 39: if(read) { return 2080374784; } else { return -2080374785; }
			case 40: if(read) { return 2113929216; } else { return -2113929217; }
			case 41: if(read) { return 2130706432; } else { return -2130706433; }
			case 42: if(read) { return 2139095040; } else { return -2139095041; }
			case 43: if(read) { return 2143289344; } else { return -2143289345; }
			case 44: if(read) { return 2145386496; } else { return -2145386497; }
			case 45: if(read) { return 2146435072; } else { return -2146435073; }
			case 46: if(read) { return 2146959360; } else { return -2146959361; }
			case 47: if(read) { return 2147221504; } else { return -2147221505; }
			case 48: if(read) { return 2147352576; } else { return -2147352577; }
			case 49: if(read) { return 2147418112; } else { return -2147418113; }
			case 50: if(read) { return 2147450880; } else { return -2147450881; }
			case 51: if(read) { return 2147467264; } else { return -2147467265; }
			case 52: if(read) { return 2147475456; } else { return -2147475457; }
			case 53: if(read) { return 2147479552; } else { return -2147479553; }
			case 54: if(read) { return 2147481600; } else { return -2147481601; }
			case 55: if(read) { return 2147482624; } else { return -2147482625; }
			case 56: if(read) { return 2147483136; } else { return -2147483137; }
			case 57: if(read) { return 2147483392; } else { return -2147483393; }
			case 58: if(read) { return 2147483520; } else { return -2147483521; }
			case 59: if(read) { return 2147483584; } else { return -2147483585; }
			case 60: if(read) { return 2147483616; } else { return -2147483617; }
			case 61: if(read) { return 2147483632; } else { return -2147483633; }
			case 62: if(read) { return 2147483640; } else { return -2147483641; }
			case 63: if(read) { return 2147483644; } else { return -2147483645; }
			case 64: if(read) { return 2147483646; } else { return -2147483647; }
			case 65: if(read) { return 2147483647; } else { return -2147483648; }
			case 68: if(read) { return 0; } else { return -1; }
			case 69: if(read) { return 536870912; } else { return -536870913; }
			case 70: if(read) { return 805306368; } else { return -805306369; }
			case 71: if(read) { return 939524096; } else { return -939524097; }
			case 72: if(read) { return 1006632960; } else { return -1006632961; }
			case 73: if(read) { return 1040187392; } else { return -1040187393; }
			case 74: if(read) { return 1056964608; } else { return -1056964609; }
			case 75: if(read) { return 1065353216; } else { return -1065353217; }
			case 76: if(read) { return 1069547520; } else { return -1069547521; }
			case 77: if(read) { return 1071644672; } else { return -1071644673; }
			case 78: if(read) { return 1072693248; } else { return -1072693249; }
			case 79: if(read) { return 1073217536; } else { return -1073217537; }
			case 80: if(read) { return 1073479680; } else { return -1073479681; }
			case 81: if(read) { return 1073610752; } else { return -1073610753; }
			case 82: if(read) { return 1073676288; } else { return -1073676289; }
			case 83: if(read) { return 1073709056; } else { return -1073709057; }
			case 84: if(read) { return 1073725440; } else { return -1073725441; }
			case 85: if(read) { return 1073733632; } else { return -1073733633; }
			case 86: if(read) { return 1073737728; } else { return -1073737729; }
			case 87: if(read) { return 1073739776; } else { return -1073739777; }
			case 88: if(read) { return 1073740800; } else { return -1073740801; }
			case 89: if(read) { return 1073741312; } else { return -1073741313; }
			case 90: if(read) { return 1073741568; } else { return -1073741569; }
			case 91: if(read) { return 1073741696; } else { return -1073741697; }
			case 92: if(read) { return 1073741760; } else { return -1073741761; }
			case 93: if(read) { return 1073741792; } else { return -1073741793; }
			case 94: if(read) { return 1073741808; } else { return -1073741809; }
			case 95: if(read) { return 1073741816; } else { return -1073741817; }
			case 96: if(read) { return 1073741820; } else { return -1073741821; }
			case 97: if(read) { return 1073741822; } else { return -1073741823; }
			case 98: if(read) { return 1073741823; } else { return -1073741824; }
			case 102: if(read) { return 0; } else { return -1; }
			case 103: if(read) { return 268435456; } else { return -268435457; }
			case 104: if(read) { return 402653184; } else { return -402653185; }
			case 105: if(read) { return 469762048; } else { return -469762049; }
			case 106: if(read) { return 503316480; } else { return -503316481; }
			case 107: if(read) { return 520093696; } else { return -520093697; }
			case 108: if(read) { return 528482304; } else { return -528482305; }
			case 109: if(read) { return 532676608; } else { return -532676609; }
			case 110: if(read) { return 534773760; } else { return -534773761; }
			case 111: if(read) { return 535822336; } else { return -535822337; }
			case 112: if(read) { return 536346624; } else { return -536346625; }
			case 113: if(read) { return 536608768; } else { return -536608769; }
			case 114: if(read) { return 536739840; } else { return -536739841; }
			case 115: if(read) { return 536805376; } else { return -536805377; }
			case 116: if(read) { return 536838144; } else { return -536838145; }
			case 117: if(read) { return 536854528; } else { return -536854529; }
			case 118: if(read) { return 536862720; } else { return -536862721; }
			case 119: if(read) { return 536866816; } else { return -536866817; }
			case 120: if(read) { return 536868864; } else { return -536868865; }
			case 121: if(read) { return 536869888; } else { return -536869889; }
			case 122: if(read) { return 536870400; } else { return -536870401; }
			case 123: if(read) { return 536870656; } else { return -536870657; }
			case 124: if(read) { return 536870784; } else { return -536870785; }
			case 125: if(read) { return 536870848; } else { return -536870849; }
			case 126: if(read) { return 536870880; } else { return -536870881; }
			case 127: if(read) { return 536870896; } else { return -536870897; }
			case 128: if(read) { return 536870904; } else { return -536870905; }
			case 129: if(read) { return 536870908; } else { return -536870909; }
			case 130: if(read) { return 536870910; } else { return -536870911; }
			case 131: if(read) { return 536870911; } else { return -536870912; }
			case 136: if(read) { return 0; } else { return -1; }
			case 137: if(read) { return 134217728; } else { return -134217729; }
			case 138: if(read) { return 201326592; } else { return -201326593; }
			case 139: if(read) { return 234881024; } else { return -234881025; }
			case 140: if(read) { return 251658240; } else { return -251658241; }
			case 141: if(read) { return 260046848; } else { return -260046849; }
			case 142: if(read) { return 264241152; } else { return -264241153; }
			case 143: if(read) { return 266338304; } else { return -266338305; }
			case 144: if(read) { return 267386880; } else { return -267386881; }
			case 145: if(read) { return 267911168; } else { return -267911169; }
			case 146: if(read) { return 268173312; } else { return -268173313; }
			case 147: if(read) { return 268304384; } else { return -268304385; }
			case 148: if(read) { return 268369920; } else { return -268369921; }
			case 149: if(read) { return 268402688; } else { return -268402689; }
			case 150: if(read) { return 268419072; } else { return -268419073; }
			case 151: if(read) { return 268427264; } else { return -268427265; }
			case 152: if(read) { return 268431360; } else { return -268431361; }
			case 153: if(read) { return 268433408; } else { return -268433409; }
			case 154: if(read) { return 268434432; } else { return -268434433; }
			case 155: if(read) { return 268434944; } else { return -268434945; }
			case 156: if(read) { return 268435200; } else { return -268435201; }
			case 157: if(read) { return 268435328; } else { return -268435329; }
			case 158: if(read) { return 268435392; } else { return -268435393; }
			case 159: if(read) { return 268435424; } else { return -268435425; }
			case 160: if(read) { return 268435440; } else { return -268435441; }
			case 161: if(read) { return 268435448; } else { return -268435449; }
			case 162: if(read) { return 268435452; } else { return -268435453; }
			case 163: if(read) { return 268435454; } else { return -268435455; }
			case 164: if(read) { return 268435455; } else { return -268435456; }
			case 170: if(read) { return 0; } else { return -1; }
			case 171: if(read) { return 67108864; } else { return -67108865; }
			case 172: if(read) { return 100663296; } else { return -100663297; }
			case 173: if(read) { return 117440512; } else { return -117440513; }
			case 174: if(read) { return 125829120; } else { return -125829121; }
			case 175: if(read) { return 130023424; } else { return -130023425; }
			case 176: if(read) { return 132120576; } else { return -132120577; }
			case 177: if(read) { return 133169152; } else { return -133169153; }
			case 178: if(read) { return 133693440; } else { return -133693441; }
			case 179: if(read) { return 133955584; } else { return -133955585; }
			case 180: if(read) { return 134086656; } else { return -134086657; }
			case 181: if(read) { return 134152192; } else { return -134152193; }
			case 182: if(read) { return 134184960; } else { return -134184961; }
			case 183: if(read) { return 134201344; } else { return -134201345; }
			case 184: if(read) { return 134209536; } else { return -134209537; }
			case 185: if(read) { return 134213632; } else { return -134213633; }
			case 186: if(read) { return 134215680; } else { return -134215681; }
			case 187: if(read) { return 134216704; } else { return -134216705; }
			case 188: if(read) { return 134217216; } else { return -134217217; }
			case 189: if(read) { return 134217472; } else { return -134217473; }
			case 190: if(read) { return 134217600; } else { return -134217601; }
			case 191: if(read) { return 134217664; } else { return -134217665; }
			case 192: if(read) { return 134217696; } else { return -134217697; }
			case 193: if(read) { return 134217712; } else { return -134217713; }
			case 194: if(read) { return 134217720; } else { return -134217721; }
			case 195: if(read) { return 134217724; } else { return -134217725; }
			case 196: if(read) { return 134217726; } else { return -134217727; }
			case 197: if(read) { return 134217727; } else { return -134217728; }
			case 204: if(read) { return 0; } else { return -1; }
			case 205: if(read) { return 33554432; } else { return -33554433; }
			case 206: if(read) { return 50331648; } else { return -50331649; }
			case 207: if(read) { return 58720256; } else { return -58720257; }
			case 208: if(read) { return 62914560; } else { return -62914561; }
			case 209: if(read) { return 65011712; } else { return -65011713; }
			case 210: if(read) { return 66060288; } else { return -66060289; }
			case 211: if(read) { return 66584576; } else { return -66584577; }
			case 212: if(read) { return 66846720; } else { return -66846721; }
			case 213: if(read) { return 66977792; } else { return -66977793; }
			case 214: if(read) { return 67043328; } else { return -67043329; }
			case 215: if(read) { return 67076096; } else { return -67076097; }
			case 216: if(read) { return 67092480; } else { return -67092481; }
			case 217: if(read) { return 67100672; } else { return -67100673; }
			case 218: if(read) { return 67104768; } else { return -67104769; }
			case 219: if(read) { return 67106816; } else { return -67106817; }
			case 220: if(read) { return 67107840; } else { return -67107841; }
			case 221: if(read) { return 67108352; } else { return -67108353; }
			case 222: if(read) { return 67108608; } else { return -67108609; }
			case 223: if(read) { return 67108736; } else { return -67108737; }
			case 224: if(read) { return 67108800; } else { return -67108801; }
			case 225: if(read) { return 67108832; } else { return -67108833; }
			case 226: if(read) { return 67108848; } else { return -67108849; }
			case 227: if(read) { return 67108856; } else { return -67108857; }
			case 228: if(read) { return 67108860; } else { return -67108861; }
			case 229: if(read) { return 67108862; } else { return -67108863; }
			case 230: if(read) { return 67108863; } else { return -67108864; }
			case 238: if(read) { return 0; } else { return -1; }
			case 239: if(read) { return 16777216; } else { return -16777217; }
			case 240: if(read) { return 25165824; } else { return -25165825; }
			case 241: if(read) { return 29360128; } else { return -29360129; }
			case 242: if(read) { return 31457280; } else { return -31457281; }
			case 243: if(read) { return 32505856; } else { return -32505857; }
			case 244: if(read) { return 33030144; } else { return -33030145; }
			case 245: if(read) { return 33292288; } else { return -33292289; }
			case 246: if(read) { return 33423360; } else { return -33423361; }
			case 247: if(read) { return 33488896; } else { return -33488897; }
			case 248: if(read) { return 33521664; } else { return -33521665; }
			case 249: if(read) { return 33538048; } else { return -33538049; }
			case 250: if(read) { return 33546240; } else { return -33546241; }
			case 251: if(read) { return 33550336; } else { return -33550337; }
			case 252: if(read) { return 33552384; } else { return -33552385; }
			case 253: if(read) { return 33553408; } else { return -33553409; }
			case 254: if(read) { return 33553920; } else { return -33553921; }
			case 255: if(read) { return 33554176; } else { return -33554177; }
			case 256: if(read) { return 33554304; } else { return -33554305; }
			case 257: if(read) { return 33554368; } else { return -33554369; }
			case 258: if(read) { return 33554400; } else { return -33554401; }
			case 259: if(read) { return 33554416; } else { return -33554417; }
			case 260: if(read) { return 33554424; } else { return -33554425; }
			case 261: if(read) { return 33554428; } else { return -33554429; }
			case 262: if(read) { return 33554430; } else { return -33554431; }
			case 263: if(read) { return 33554431; } else { return -33554432; }
			case 272: if(read) { return 0; } else { return -1; }
			case 273: if(read) { return 8388608; } else { return -8388609; }
			case 274: if(read) { return 12582912; } else { return -12582913; }
			case 275: if(read) { return 14680064; } else { return -14680065; }
			case 276: if(read) { return 15728640; } else { return -15728641; }
			case 277: if(read) { return 16252928; } else { return -16252929; }
			case 278: if(read) { return 16515072; } else { return -16515073; }
			case 279: if(read) { return 16646144; } else { return -16646145; }
			case 280: if(read) { return 16711680; } else { return -16711681; }
			case 281: if(read) { return 16744448; } else { return -16744449; }
			case 282: if(read) { return 16760832; } else { return -16760833; }
			case 283: if(read) { return 16769024; } else { return -16769025; }
			case 284: if(read) { return 16773120; } else { return -16773121; }
			case 285: if(read) { return 16775168; } else { return -16775169; }
			case 286: if(read) { return 16776192; } else { return -16776193; }
			case 287: if(read) { return 16776704; } else { return -16776705; }
			case 288: if(read) { return 16776960; } else { return -16776961; }
			case 289: if(read) { return 16777088; } else { return -16777089; }
			case 290: if(read) { return 16777152; } else { return -16777153; }
			case 291: if(read) { return 16777184; } else { return -16777185; }
			case 292: if(read) { return 16777200; } else { return -16777201; }
			case 293: if(read) { return 16777208; } else { return -16777209; }
			case 294: if(read) { return 16777212; } else { return -16777213; }
			case 295: if(read) { return 16777214; } else { return -16777215; }
			case 296: if(read) { return 16777215; } else { return -16777216; }
			case 306: if(read) { return 0; } else { return -1; }
			case 307: if(read) { return 4194304; } else { return -4194305; }
			case 308: if(read) { return 6291456; } else { return -6291457; }
			case 309: if(read) { return 7340032; } else { return -7340033; }
			case 310: if(read) { return 7864320; } else { return -7864321; }
			case 311: if(read) { return 8126464; } else { return -8126465; }
			case 312: if(read) { return 8257536; } else { return -8257537; }
			case 313: if(read) { return 8323072; } else { return -8323073; }
			case 314: if(read) { return 8355840; } else { return -8355841; }
			case 315: if(read) { return 8372224; } else { return -8372225; }
			case 316: if(read) { return 8380416; } else { return -8380417; }
			case 317: if(read) { return 8384512; } else { return -8384513; }
			case 318: if(read) { return 8386560; } else { return -8386561; }
			case 319: if(read) { return 8387584; } else { return -8387585; }
			case 320: if(read) { return 8388096; } else { return -8388097; }
			case 321: if(read) { return 8388352; } else { return -8388353; }
			case 322: if(read) { return 8388480; } else { return -8388481; }
			case 323: if(read) { return 8388544; } else { return -8388545; }
			case 324: if(read) { return 8388576; } else { return -8388577; }
			case 325: if(read) { return 8388592; } else { return -8388593; }
			case 326: if(read) { return 8388600; } else { return -8388601; }
			case 327: if(read) { return 8388604; } else { return -8388605; }
			case 328: if(read) { return 8388606; } else { return -8388607; }
			case 329: if(read) { return 8388607; } else { return -8388608; }
			case 340: if(read) { return 0; } else { return -1; }
			case 341: if(read) { return 2097152; } else { return -2097153; }
			case 342: if(read) { return 3145728; } else { return -3145729; }
			case 343: if(read) { return 3670016; } else { return -3670017; }
			case 344: if(read) { return 3932160; } else { return -3932161; }
			case 345: if(read) { return 4063232; } else { return -4063233; }
			case 346: if(read) { return 4128768; } else { return -4128769; }
			case 347: if(read) { return 4161536; } else { return -4161537; }
			case 348: if(read) { return 4177920; } else { return -4177921; }
			case 349: if(read) { return 4186112; } else { return -4186113; }
			case 350: if(read) { return 4190208; } else { return -4190209; }
			case 351: if(read) { return 4192256; } else { return -4192257; }
			case 352: if(read) { return 4193280; } else { return -4193281; }
			case 353: if(read) { return 4193792; } else { return -4193793; }
			case 354: if(read) { return 4194048; } else { return -4194049; }
			case 355: if(read) { return 4194176; } else { return -4194177; }
			case 356: if(read) { return 4194240; } else { return -4194241; }
			case 357: if(read) { return 4194272; } else { return -4194273; }
			case 358: if(read) { return 4194288; } else { return -4194289; }
			case 359: if(read) { return 4194296; } else { return -4194297; }
			case 360: if(read) { return 4194300; } else { return -4194301; }
			case 361: if(read) { return 4194302; } else { return -4194303; }
			case 362: if(read) { return 4194303; } else { return -4194304; }
			case 374: if(read) { return 0; } else { return -1; }
			case 375: if(read) { return 1048576; } else { return -1048577; }
			case 376: if(read) { return 1572864; } else { return -1572865; }
			case 377: if(read) { return 1835008; } else { return -1835009; }
			case 378: if(read) { return 1966080; } else { return -1966081; }
			case 379: if(read) { return 2031616; } else { return -2031617; }
			case 380: if(read) { return 2064384; } else { return -2064385; }
			case 381: if(read) { return 2080768; } else { return -2080769; }
			case 382: if(read) { return 2088960; } else { return -2088961; }
			case 383: if(read) { return 2093056; } else { return -2093057; }
			case 384: if(read) { return 2095104; } else { return -2095105; }
			case 385: if(read) { return 2096128; } else { return -2096129; }
			case 386: if(read) { return 2096640; } else { return -2096641; }
			case 387: if(read) { return 2096896; } else { return -2096897; }
			case 388: if(read) { return 2097024; } else { return -2097025; }
			case 389: if(read) { return 2097088; } else { return -2097089; }
			case 390: if(read) { return 2097120; } else { return -2097121; }
			case 391: if(read) { return 2097136; } else { return -2097137; }
			case 392: if(read) { return 2097144; } else { return -2097145; }
			case 393: if(read) { return 2097148; } else { return -2097149; }
			case 394: if(read) { return 2097150; } else { return -2097151; }
			case 395: if(read) { return 2097151; } else { return -2097152; }
			case 408: if(read) { return 0; } else { return -1; }
			case 409: if(read) { return 524288; } else { return -524289; }
			case 410: if(read) { return 786432; } else { return -786433; }
			case 411: if(read) { return 917504; } else { return -917505; }
			case 412: if(read) { return 983040; } else { return -983041; }
			case 413: if(read) { return 1015808; } else { return -1015809; }
			case 414: if(read) { return 1032192; } else { return -1032193; }
			case 415: if(read) { return 1040384; } else { return -1040385; }
			case 416: if(read) { return 1044480; } else { return -1044481; }
			case 417: if(read) { return 1046528; } else { return -1046529; }
			case 418: if(read) { return 1047552; } else { return -1047553; }
			case 419: if(read) { return 1048064; } else { return -1048065; }
			case 420: if(read) { return 1048320; } else { return -1048321; }
			case 421: if(read) { return 1048448; } else { return -1048449; }
			case 422: if(read) { return 1048512; } else { return -1048513; }
			case 423: if(read) { return 1048544; } else { return -1048545; }
			case 424: if(read) { return 1048560; } else { return -1048561; }
			case 425: if(read) { return 1048568; } else { return -1048569; }
			case 426: if(read) { return 1048572; } else { return -1048573; }
			case 427: if(read) { return 1048574; } else { return -1048575; }
			case 428: if(read) { return 1048575; } else { return -1048576; }
			case 442: if(read) { return 0; } else { return -1; }
			case 443: if(read) { return 262144; } else { return -262145; }
			case 444: if(read) { return 393216; } else { return -393217; }
			case 445: if(read) { return 458752; } else { return -458753; }
			case 446: if(read) { return 491520; } else { return -491521; }
			case 447: if(read) { return 507904; } else { return -507905; }
			case 448: if(read) { return 516096; } else { return -516097; }
			case 449: if(read) { return 520192; } else { return -520193; }
			case 450: if(read) { return 522240; } else { return -522241; }
			case 451: if(read) { return 523264; } else { return -523265; }
			case 452: if(read) { return 523776; } else { return -523777; }
			case 453: if(read) { return 524032; } else { return -524033; }
			case 454: if(read) { return 524160; } else { return -524161; }
			case 455: if(read) { return 524224; } else { return -524225; }
			case 456: if(read) { return 524256; } else { return -524257; }
			case 457: if(read) { return 524272; } else { return -524273; }
			case 458: if(read) { return 524280; } else { return -524281; }
			case 459: if(read) { return 524284; } else { return -524285; }
			case 460: if(read) { return 524286; } else { return -524287; }
			case 461: if(read) { return 524287; } else { return -524288; }
			case 476: if(read) { return 0; } else { return -1; }
			case 477: if(read) { return 131072; } else { return -131073; }
			case 478: if(read) { return 196608; } else { return -196609; }
			case 479: if(read) { return 229376; } else { return -229377; }
			case 480: if(read) { return 245760; } else { return -245761; }
			case 481: if(read) { return 253952; } else { return -253953; }
			case 482: if(read) { return 258048; } else { return -258049; }
			case 483: if(read) { return 260096; } else { return -260097; }
			case 484: if(read) { return 261120; } else { return -261121; }
			case 485: if(read) { return 261632; } else { return -261633; }
			case 486: if(read) { return 261888; } else { return -261889; }
			case 487: if(read) { return 262016; } else { return -262017; }
			case 488: if(read) { return 262080; } else { return -262081; }
			case 489: if(read) { return 262112; } else { return -262113; }
			case 490: if(read) { return 262128; } else { return -262129; }
			case 491: if(read) { return 262136; } else { return -262137; }
			case 492: if(read) { return 262140; } else { return -262141; }
			case 493: if(read) { return 262142; } else { return -262143; }
			case 494: if(read) { return 262143; } else { return -262144; }
			case 510: if(read) { return 0; } else { return -1; }
			case 511: if(read) { return 65536; } else { return -65537; }
			case 512: if(read) { return 98304; } else { return -98305; }
			case 513: if(read) { return 114688; } else { return -114689; }
			case 514: if(read) { return 122880; } else { return -122881; }
			case 515: if(read) { return 126976; } else { return -126977; }
			case 516: if(read) { return 129024; } else { return -129025; }
			case 517: if(read) { return 130048; } else { return -130049; }
			case 518: if(read) { return 130560; } else { return -130561; }
			case 519: if(read) { return 130816; } else { return -130817; }
			case 520: if(read) { return 130944; } else { return -130945; }
			case 521: if(read) { return 131008; } else { return -131009; }
			case 522: if(read) { return 131040; } else { return -131041; }
			case 523: if(read) { return 131056; } else { return -131057; }
			case 524: if(read) { return 131064; } else { return -131065; }
			case 525: if(read) { return 131068; } else { return -131069; }
			case 526: if(read) { return 131070; } else { return -131071; }
			case 527: if(read) { return 131071; } else { return -131072; }
			case 544: if(read) { return 0; } else { return -1; }
			case 545: if(read) { return 32768; } else { return -32769; }
			case 546: if(read) { return 49152; } else { return -49153; }
			case 547: if(read) { return 57344; } else { return -57345; }
			case 548: if(read) { return 61440; } else { return -61441; }
			case 549: if(read) { return 63488; } else { return -63489; }
			case 550: if(read) { return 64512; } else { return -64513; }
			case 551: if(read) { return 65024; } else { return -65025; }
			case 552: if(read) { return 65280; } else { return -65281; }
			case 553: if(read) { return 65408; } else { return -65409; }
			case 554: if(read) { return 65472; } else { return -65473; }
			case 555: if(read) { return 65504; } else { return -65505; }
			case 556: if(read) { return 65520; } else { return -65521; }
			case 557: if(read) { return 65528; } else { return -65529; }
			case 558: if(read) { return 65532; } else { return -65533; }
			case 559: if(read) { return 65534; } else { return -65535; }
			case 560: if(read) { return 65535; } else { return -65536; }
			case 578: if(read) { return 0; } else { return -1; }
			case 579: if(read) { return 16384; } else { return -16385; }
			case 580: if(read) { return 24576; } else { return -24577; }
			case 581: if(read) { return 28672; } else { return -28673; }
			case 582: if(read) { return 30720; } else { return -30721; }
			case 583: if(read) { return 31744; } else { return -31745; }
			case 584: if(read) { return 32256; } else { return -32257; }
			case 585: if(read) { return 32512; } else { return -32513; }
			case 586: if(read) { return 32640; } else { return -32641; }
			case 587: if(read) { return 32704; } else { return -32705; }
			case 588: if(read) { return 32736; } else { return -32737; }
			case 589: if(read) { return 32752; } else { return -32753; }
			case 590: if(read) { return 32760; } else { return -32761; }
			case 591: if(read) { return 32764; } else { return -32765; }
			case 592: if(read) { return 32766; } else { return -32767; }
			case 593: if(read) { return 32767; } else { return -32768; }
			case 612: if(read) { return 0; } else { return -1; }
			case 613: if(read) { return 8192; } else { return -8193; }
			case 614: if(read) { return 12288; } else { return -12289; }
			case 615: if(read) { return 14336; } else { return -14337; }
			case 616: if(read) { return 15360; } else { return -15361; }
			case 617: if(read) { return 15872; } else { return -15873; }
			case 618: if(read) { return 16128; } else { return -16129; }
			case 619: if(read) { return 16256; } else { return -16257; }
			case 620: if(read) { return 16320; } else { return -16321; }
			case 621: if(read) { return 16352; } else { return -16353; }
			case 622: if(read) { return 16368; } else { return -16369; }
			case 623: if(read) { return 16376; } else { return -16377; }
			case 624: if(read) { return 16380; } else { return -16381; }
			case 625: if(read) { return 16382; } else { return -16383; }
			case 626: if(read) { return 16383; } else { return -16384; }
			case 646: if(read) { return 0; } else { return -1; }
			case 647: if(read) { return 4096; } else { return -4097; }
			case 648: if(read) { return 6144; } else { return -6145; }
			case 649: if(read) { return 7168; } else { return -7169; }
			case 650: if(read) { return 7680; } else { return -7681; }
			case 651: if(read) { return 7936; } else { return -7937; }
			case 652: if(read) { return 8064; } else { return -8065; }
			case 653: if(read) { return 8128; } else { return -8129; }
			case 654: if(read) { return 8160; } else { return -8161; }
			case 655: if(read) { return 8176; } else { return -8177; }
			case 656: if(read) { return 8184; } else { return -8185; }
			case 657: if(read) { return 8188; } else { return -8189; }
			case 658: if(read) { return 8190; } else { return -8191; }
			case 659: if(read) { return 8191; } else { return -8192; }
			case 680: if(read) { return 0; } else { return -1; }
			case 681: if(read) { return 2048; } else { return -2049; }
			case 682: if(read) { return 3072; } else { return -3073; }
			case 683: if(read) { return 3584; } else { return -3585; }
			case 684: if(read) { return 3840; } else { return -3841; }
			case 685: if(read) { return 3968; } else { return -3969; }
			case 686: if(read) { return 4032; } else { return -4033; }
			case 687: if(read) { return 4064; } else { return -4065; }
			case 688: if(read) { return 4080; } else { return -4081; }
			case 689: if(read) { return 4088; } else { return -4089; }
			case 690: if(read) { return 4092; } else { return -4093; }
			case 691: if(read) { return 4094; } else { return -4095; }
			case 692: if(read) { return 4095; } else { return -4096; }
			case 714: if(read) { return 0; } else { return -1; }
			case 715: if(read) { return 1024; } else { return -1025; }
			case 716: if(read) { return 1536; } else { return -1537; }
			case 717: if(read) { return 1792; } else { return -1793; }
			case 718: if(read) { return 1920; } else { return -1921; }
			case 719: if(read) { return 1984; } else { return -1985; }
			case 720: if(read) { return 2016; } else { return -2017; }
			case 721: if(read) { return 2032; } else { return -2033; }
			case 722: if(read) { return 2040; } else { return -2041; }
			case 723: if(read) { return 2044; } else { return -2045; }
			case 724: if(read) { return 2046; } else { return -2047; }
			case 725: if(read) { return 2047; } else { return -2048; }
			case 748: if(read) { return 0; } else { return -1; }
			case 749: if(read) { return 512; } else { return -513; }
			case 750: if(read) { return 768; } else { return -769; }
			case 751: if(read) { return 896; } else { return -897; }
			case 752: if(read) { return 960; } else { return -961; }
			case 753: if(read) { return 992; } else { return -993; }
			case 754: if(read) { return 1008; } else { return -1009; }
			case 755: if(read) { return 1016; } else { return -1017; }
			case 756: if(read) { return 1020; } else { return -1021; }
			case 757: if(read) { return 1022; } else { return -1023; }
			case 758: if(read) { return 1023; } else { return -1024; }
			case 782: if(read) { return 0; } else { return -1; }
			case 783: if(read) { return 256; } else { return -257; }
			case 784: if(read) { return 384; } else { return -385; }
			case 785: if(read) { return 448; } else { return -449; }
			case 786: if(read) { return 480; } else { return -481; }
			case 787: if(read) { return 496; } else { return -497; }
			case 788: if(read) { return 504; } else { return -505; }
			case 789: if(read) { return 508; } else { return -509; }
			case 790: if(read) { return 510; } else { return -511; }
			case 791: if(read) { return 511; } else { return -512; }
			case 816: if(read) { return 0; } else { return -1; }
			case 817: if(read) { return 128; } else { return -129; }
			case 818: if(read) { return 192; } else { return -193; }
			case 819: if(read) { return 224; } else { return -225; }
			case 820: if(read) { return 240; } else { return -241; }
			case 821: if(read) { return 248; } else { return -249; }
			case 822: if(read) { return 252; } else { return -253; }
			case 823: if(read) { return 254; } else { return -255; }
			case 824: if(read) { return 255; } else { return -256; }
			case 850: if(read) { return 0; } else { return -1; }
			case 851: if(read) { return 64; } else { return -65; }
			case 852: if(read) { return 96; } else { return -97; }
			case 853: if(read) { return 112; } else { return -113; }
			case 854: if(read) { return 120; } else { return -121; }
			case 855: if(read) { return 124; } else { return -125; }
			case 856: if(read) { return 126; } else { return -127; }
			case 857: if(read) { return 127; } else { return -128; }
			case 884: if(read) { return 0; } else { return -1; }
			case 885: if(read) { return 32; } else { return -33; }
			case 886: if(read) { return 48; } else { return -49; }
			case 887: if(read) { return 56; } else { return -57; }
			case 888: if(read) { return 60; } else { return -61; }
			case 889: if(read) { return 62; } else { return -63; }
			case 890: if(read) { return 63; } else { return -64; }
			case 918: if(read) { return 0; } else { return -1; }
			case 919: if(read) { return 16; } else { return -17; }
			case 920: if(read) { return 24; } else { return -25; }
			case 921: if(read) { return 28; } else { return -29; }
			case 922: if(read) { return 30; } else { return -31; }
			case 923: if(read) { return 31; } else { return -32; }
			case 952: if(read) { return 0; } else { return -1; }
			case 953: if(read) { return 8; } else { return -9; }
			case 954: if(read) { return 12; } else { return -13; }
			case 955: if(read) { return 14; } else { return -15; }
			case 956: if(read) { return 15; } else { return -16; }
			case 986: if(read) { return 0; } else { return -1; }
			case 987: if(read) { return 4; } else { return -5; }
			case 988: if(read) { return 6; } else { return -7; }
			case 989: if(read) { return 7; } else { return -8; }
			case 1020: if(read) { return 0; } else { return -1; }
			case 1021: if(read) { return 2; } else { return -3; }
			case 1022: if(read) { return 3; } else { return -4; }
			case 1054: if(read) { return 0; } else { return -1; }
			case 1055: if(read) { return 1; } else { return -2; }
        }
        return 0;
    }
}
