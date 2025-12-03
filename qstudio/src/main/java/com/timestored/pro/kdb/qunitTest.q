system "d .qunitTest";

testMock:{ 
    @[`.;`f;:;{x}];
    .qunit.mock[`f;{x+y}];
    .qunit.assertEquals[value "f";{x+y};"mocked value set"];
    .qunit.reset[];
    .qunit.assertEquals[value "f";{x};"mocked value reset"]};
    
testRepeatedMock:{
    @[`.;`f;:;{x}];
    .qunit.mock[`f;{x+1}];
    .qunit.mock[`f;{x+2}];
    .qunit.assertEquals[value `f;{x+2};"last mocked value set"];
    .qunit.reset[];
    .qunit.assertEquals[value `f;{x};"mocked value reset"]};

testResetUnsetValue:{
    delete f1 from `.;
    .qunit.mock[`f1;{x+1}];
    .qunit.reset[];
    .qunit.assertError[value;"f1";"value no longer exists"]};
    
testMockNamespacedFunction:{
    .qunit.runInNs["na"; "gg:{x+1}; ff:{gg 1}"];
    / mocking namespace function, makes new function namespaced aswell
    .qunit.mock[`.na.ff;{gg 100}];
    .qunit.assertEquals[value[".na.ff"] 0;101;"func call was mocked."];
    .qunit.reset[];
    .qunit.assertEquals[value[".na.ff"] 0;2;"func call was restored."]};
   
 
// not a proper namespace defined function, instead a "dotted" definitioin
testMockDottedFunction:{
    .qunit.runInNs[""; "B:9; .nb.hh:{A}"];
    .qunit.mock[`.nb.hh;{B}];
    msg:"mock'ing a global dot function, places new func in global NS aswell";
    .qunit.assertEquals[(value ".nb.hh") `;9;msg]};
    
    
testResetOnlySpecifiedEntities:{
    .qunit.runInNs[""; "delete f3 from `.qunitTest; g3:{x}"];
    .qunit.mock[`f3;{x}]; 
    .qunit.mock[`g3;{9}]; 
    .qunit.reset `f3;
    .qunit.assertThrows[value;"f3";"value no longer exists"];
    .qunit.assertEquals[(value "g3") 1;9;"nonreset var still exists"]};
    
testTableDiff:{
    t:([] n:til 6; primes:1 2 3 5 7 9; name:`one`two`three`five`seven`nine);
    .qunit.assertEquals[t 2 3 5 1 1 1; t; "This test is meant to fail."]};
    