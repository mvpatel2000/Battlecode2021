def twos_comp(val, bits):
    """compute the 2's complement of int value val"""
    if (val & (1 << (bits - 1))) != 0: # if sign bit is set e.g., 8bit: 128-255
        val = val - (1 << bits)        # compute negative value
    return val                         # return positive value as is

with open('bitmask_cases.txt', 'w') as f:
    for i in range(32):
        for j in range(i, 33):
            read_mask = '0'*i + '1'*(j-i) + '0'*(32-j)
            read_mask = twos_comp(int(read_mask,2), 32)
            write_mask = -1 - read_mask
            f.write('\t\t\tcase '+str(33*i+j)+': if(read) { return ' + str(read_mask) + '; } else { return ' + str(write_mask) + '; }\n')