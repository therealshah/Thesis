Morph datasets are files that are generated randomly by running a program. We first generated two random files, each of length 20MB. We then created a new file by “morphing” the two files that were generated. The morphing was done by setting parameters p and q which defined the probability of staying in state1 and state2, respectively. The states represented which of the input files we read from next. State1 corresponded to file1 and state2 corresponded to file2. If we were in state1, we stayed in state1 with a probability of p and switched to state2 with a probability of 1-p. If we were in state2, we stayed in state2 with a probability of q and switched to state1 with a probability of 1-q.
Depending on which state we were in, we copied byte i  to the output morph file from the corresponding input file, where i  is the current iteration of this step. The next state was computed after each copy as discussed above. We repeated this until we reached the end of the smaller input file and thus, the size of the output file was min(file1.length, file2.length). 




We also generated periodic datasets by tweaking the morph code.