package checks;

class ArrayForVarArgCheckBar {}
class ArrayForVarArgCheckFoo extends ArrayForVarArgCheckBar {}
class ArrayForVarArgCheck {

  public void callTheThing(String s) {
    ArrayForVarArgCheckFoo foo = new ArrayForVarArgCheckFoo();

    doTheThing2(new ArrayForVarArgCheckFoo[] {foo, foo});  // Noncompliant {{Disambiguate this call by either casting as "ArrayForVarArgCheckBar" or "ArrayForVarArgCheckBar[]".}}
    unknown(new ArrayForVarArgCheckFoo[0]);
  }

  public void doTheThing2 (ArrayForVarArgCheckBar... args) {
  }

}
