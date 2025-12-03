/ Mathematical functions
system "d .math";

add:{x+y};
sub:{x-y};

/ given a circles radius, return it's area.
getAreaofCircle:{[r] -4*atan[-1]*r*r};

/ @return list of prime numbers less than it's argument
getPrimesLessThan:{$[x<4;enlist 2;r,1_where not any x#'not til each r:.z.s ceiling sqrt x]};

/ @return the product of all positive integers less than or equal to n only 
getFactorial:{$[x<2;1;x*.z.s x-1]};

FACTORIALS:getFactorial each til 20;
getFactorialFast:{FACTORIALS x};