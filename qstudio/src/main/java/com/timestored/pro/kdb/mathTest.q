/ QUnit testing mathematical functions
system "d .mathTest";

testAdd:{.qunit.assertEquals[.math.add[2;2]; 4; "2 plus 2 equals four"]};

/ we use assert error as a projection to check an error is thrown for `two+2
testAddSymbol:{.qunit.assertError[.math.add[2;]; `two; "cant add symbol to int"]};

testSub:{ .qunit.assertTrue[.math.sub[2;2]~0; "2 minus 2 equals zero"] };

/ assert that allows using any relational operator for 
/ comparing actual and expected values.
testGetAreaofCircle:{ 
    r:.math.getAreaofCircle[1];
    .qunit.assertThat[r;<;3.1417; "nearly pi <"];
    .qunit.assertThat[r;>;3.1415; "nearly pi >"]};

testGetPrimesLessThanTen:{ 
    r:.math.getPrimesLessThan[10];
    .qunit.assertEquals[r; 2 3 5 7; "primes < 10 match"]};
    
testGetPrimesLessThanMinusOne:{ 
    r:.math.getPrimesLessThan[-1];
    .qunit.assertEquals[r; (); "no negative primes"]};
    
testGetFactorial:{ 
    r:.math.getFactorial 20; 
    .qunit.assertEquals[r; 2432902008176640000; "known factorial matches"]};
           
/ to set a max time we use the qunitconfig
/ a dictionary from test names to test parameters to their values
testGetFactorialSpeed:{ max .math.getFactorial each 10+100000?10 };
qunitConfig:``!();
qunitConfig[`testGetFactorialSpeed]:`maxTime`maxMem!(100;20000000);  


testSubComparedToFile:{ .qunit.assertKnown[0; `testSubComparedToFile; "2 minus 2 equals zero"] };

testTableOfData:{
    t:([] v:til 12);
    t:update areaOfCircle:.math.getAreaofCircle v, factorial:.math.getFactorial each v, tiller:til each v from t;
    .qunit.assertKnown[t; `testTableOfData; "calc match known table"] };
        
testExceptionShownInUnitTests:{  'throwMe };