/*
 * Copyright (c) 2014, Aleksander Osman
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * 
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ao486.test.stack;

import ao486.test.TestUnit;
import static ao486.test.TestUnit.run_test;
import ao486.test.layers.FlagsLayer;
import ao486.test.layers.GeneralRegisterLayer;
import ao486.test.layers.HandleModeChangeLayer;
import ao486.test.layers.IOLayer;
import ao486.test.layers.InstructionLayer;
import ao486.test.layers.Layer;
import ao486.test.layers.MemoryLayer;
import ao486.test.layers.OtherLayer;
import ao486.test.layers.Pair;
import ao486.test.layers.SegmentLayer;
import ao486.test.layers.StackLayer;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Random;

public class TestENTER_any extends TestUnit implements Serializable {
    public static void main(String args[]) throws Exception {
        run_test(TestENTER_any.class);
    }
    
    //--------------------------------------------------------------------------
    @Override
    public int get_test_count() throws Exception {
        return 100;
    }
    
    @Override
    public void init() throws Exception {
        random = new Random(7 + index);
        
        long rpl = random.nextInt(4);
        
        String instruction;
        while(true) {
            layers.clear();
            
            LinkedList<Pair<Long, Long>> prohibited_list = new LinkedList<>();
            
            InstructionLayer instr = new InstructionLayer(random, prohibited_list);
            layers.add(instr);
            layers.add(new StackLayer(random, prohibited_list));
            layers.add(new OtherLayer(OtherLayer.Type.RANDOM, random));
            layers.add(new FlagsLayer(FlagsLayer.Type.RANDOM, random));
            layers.add(new GeneralRegisterLayer(random));
            layers.add(new SegmentLayer(random));
            layers.add(new MemoryLayer(random));
            layers.add(new IOLayer(random));
            layers.addFirst(new HandleModeChangeLayer(
                    getInput("cr0_pe"),
                    getInput("vmflag"),
                    rpl, //getInput("cs_rpl"),
                    getInput("cs_p"),
                    getInput("cs_s"),
                    getInput("cs_type")
            ));
            
            // instruction size
            boolean cs_d_b = getInput("cs_d_b") == 1;
            
            boolean a32 = random.nextBoolean();
            boolean o32 = random.nextBoolean();
            
            final Random random_final = random;
            layers.addFirst(new Layer() {
                long ebp() throws Exception {
                    int val = random_final.nextInt();
                    return ((val % 18) == 0)? 0 :
                           ((val % 18) == 1)? 1 :
                           ((val % 18) == 2)? 2 :
                           ((val % 18) == 3)? 3 :
                           ((val % 18) == 4)? 4 : 
                           ((val % 18) == 5)? 0xFFFFFFFF : 
                           ((val % 18) == 6)? 0x0000FFFF : 
                                random_final.nextInt() & ((random_final.nextInt(3) == 0)? 0xFFFFFFFF : 0x00000FFF);
                }
                long esp() throws Exception {
                    int val = random_final.nextInt();
                    return ((val % 18) == 0)? 0 :
                           ((val % 18) == 1)? 1 :
                           ((val % 18) == 2)? 2 :
                           ((val % 18) == 3)? 3 :
                           ((val % 18) == 4)? 4 : 
                           ((val % 18) == 5)? 0xFFFFFFFF : 
                           ((val % 18) == 6)? 0x0000FFFF : 
                                random_final.nextInt() & ((random_final.nextInt(3) == 0)? 0xFFFFFFFF : 0x00000FFF);
                }
            });
            
            // instruction
            instruction = prepare_instr(cs_d_b, a32, o32, null);
            
            instruction += instruction;
            instruction += "0F0F";
            
            // add instruction
            instr.add_instruction(instruction);
            
            // end condition
            break;
        }
        
        System.out.println("Instruction: [" + instruction + "]");
    }
    
    String prepare_instr(boolean cs_d_b, boolean a32, boolean o32, byte modregrm_bytes[]) throws Exception {
        
        int opcodes[] = {
            0xC8
        };
        
        String prefix = "";
        if(cs_d_b != o32) { prefix = "66" + prefix; }
        if(cs_d_b != a32) { prefix = "67" + prefix; }
        
        int     opcode      = opcodes[random.nextInt(opcodes.length)];
        boolean is_modregrm = false;
        
        byte possible_modregrm = (byte)random.nextInt();
        byte possible_sib      = (byte)random.nextInt();
      
        int len = (is_modregrm == false)? 1 : 1 + modregrm_len(!a32, unsigned(possible_modregrm), unsigned(possible_sib));
        len += 3; //iw ib
        
        byte instr[] = new byte[len];
        instr[0] = (byte)opcode;
        for(int i=1; i<len; i++) {
            if(i==1)        instr[1] = possible_modregrm;
            else if(i==2)   instr[2] = possible_sib;
            else            instr[i] = (byte)random.nextInt();
        }
        
        //ENTER level
        //random
        
        return prefix + bytesToHex(instr);
    }
}
