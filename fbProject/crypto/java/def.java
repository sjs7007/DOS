//https://www.youtube.com/watch?v=j1yDsfWEAlA

DHParamaterSpec dhParams = new DHParamaterSpec(G,P);

KeyPairGenerator aliceKeyGen = KeyPairGenerator.getInstance("DH","BC")
aliceKeyGen.initialize(dhParams,new SecureRandom());

//diffie hellman algo, bouncy castle provider
KeyAgreement aliceKeyAgree = KeyAgreement.getInstance("DH","BC")
KeyPair alicePair = aliceKeyGen.generateKeyPair();

aliceKeyGen.init(alicePair.getPrivate());
Key aliceKey = aliceKeyGen.doPhase(bobPair.getPublic(),true);

//bob 
KeyPairGenerator bobKeyGen = KeyPairGenerator.getInstance("DH","BC");
bobKeyGen.initialize(dhParams,new SecureRandom());

KeyAgreement bobKeyAgree = KeyAgreement.getInstance("DH","BC");
KeyPair bobPair = bobKeyGen.generateKeyPair();

bobKeyGen.init(bobPair.getPrivate());
Key bobKey = bobKeyAgree.doPhase(alicePair.getPublic(),true);