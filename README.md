# RobinHoodHashSet

Characteristics summary:

- NOT THREAD SAFE
- NO HASH DONE (you must do it yourself)
- ORDERED

Benefits :

- FAST
- MEMORY LEAN

A Open Adressing Hash Set of Long with multiple arrays using Robin Hood 
reallocation and lean memory consumption.

Instead of Hash Modulo, we use Hash Shift to be able to have a stabilized order, 
and thus have a free ordering on the keys.

IMPORTANT : The keys are stored AS IS, which means that for good performance, you must 
ensure that the keys are uniformly distributed over the range of Longs.

Instead of reallocating the whole array each time an expand() is required, 
the algorithm is the following :

- The old layout contains k OldBlocks (O)
- k NewBlocks (N) are allocated by interleaving them : ONONONON...
- A tempBlock is allocated, where each OldBlock is iteratively copied, and 
  then each of its keys is redispatch in the blocks.

In order to keep a full ordering, there is no wrapping (i.e. if there is no 
more room at the end of the memory, we do not insert the value at the beginning), 
but extra overflow blocks are allocating as required.
