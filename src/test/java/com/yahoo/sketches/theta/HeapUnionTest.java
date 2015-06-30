/*
 * Copyright 2015, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.theta;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.yahoo.sketches.Family;
import com.yahoo.sketches.memory.Memory;
import com.yahoo.sketches.memory.NativeMemory;
import com.yahoo.sketches.theta.CompactSketch;
import com.yahoo.sketches.theta.SetOperation;
import com.yahoo.sketches.theta.Union;
import com.yahoo.sketches.theta.UpdateSketch;

/**
 * @author Lee Rhodes
 */
public class HeapUnionTest {

  @Test
  public void checkExactUnionNoOverlap() {
    int lgK = 9; //512
    int k = 1 << lgK;

    UpdateSketch usk1 = UpdateSketch.builder().build(k);
    UpdateSketch usk2 = UpdateSketch.builder().build(k);
    
    for (int i=0; i<k/2; i++) usk1.update(i); //256
    for (int i=k/2; i<k; i++) usk2.update(i); //256 no overlap
    
    double usk1est = usk1.getEstimate();
    double usk2est = usk2.getEstimate();
    
    Union union = (Union)SetOperation.builder().build(k, Family.UNION);
    
    union.update(usk1);
    union.update(usk2);
    
    double exactUnionAnswer = k;
    
    CompactSketch comp1, comp2, comp3, comp4;
    double compEst;
    
    //test all the compacts
    comp1 = union.getResult(false, null); //ordered: false
    compEst = comp1.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.0);
    assertEquals(compEst, usk1est + usk2est, 0.0);

    comp2 = union.getResult(true, null); //ordered: true
    compEst = comp2.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.0);
    
    int bytes = comp2.getCurrentBytes(false);
    byte[] byteArray = new byte[bytes];
    Memory mem = new NativeMemory(byteArray);
    
    comp3 = union.getResult(false, mem);
    compEst = comp3.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.0);
    
    comp4 = union.getResult(true, mem);
    compEst = comp4.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.0);
  }
  
  @Test
  public void checkEstUnionNoOverlap() {
    int lgK = 12; //4096
    int k = 1 << lgK;
    int u = 4*k;

    UpdateSketch usk1 = UpdateSketch.builder().build(k);
    UpdateSketch usk2 = UpdateSketch.builder().build(k);
    
    for (int i=0; i<u/2; i++) usk1.update(i); //2*k
    for (int i=u/2; i<u; i++) usk2.update(i); //2*k no overlap
    
//    double usk1est = usk1.getEstimate();
//    double usk2est = usk2.getEstimate();
    
    Union union = (Union)SetOperation.builder().build(k, Family.UNION);
    
    union.update(usk1);
    union.update(usk2);
    
    double exactUnionAnswer = u;
    
    CompactSketch comp1, comp2, comp3, comp4;
    double compEst;
    
  //test all the compacts
    comp1 = union.getResult(false, null); //ordered: false
    compEst = comp1.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.05*u);

    comp2 = union.getResult(true, null); //ordered: true
    compEst = comp2.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.05*u);
    
    int bytes = comp2.getCurrentBytes(false);
    byte[] byteArray = new byte[bytes];
    Memory mem = new NativeMemory(byteArray);
    
    comp3 = union.getResult(false, mem);
    compEst = comp3.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.05*u);
    
    comp4 = union.getResult(true, mem);
    compEst = comp4.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.05*u);
  }
  
  @Test
  public void checkExactUnionWithOverlap() {
    int lgK = 9; //512
    int k = 1 << lgK;

    UpdateSketch usk1 = UpdateSketch.builder().build(k);
    UpdateSketch usk2 = UpdateSketch.builder().build(k);
    
    for (int i=0; i<k/2; i++) usk1.update(i); //256
    for (int i=0; i<k  ; i++) usk2.update(i); //512, 256 overlapped
    
    double usk1est = usk1.getEstimate();
    double usk2est = usk2.getEstimate();
    
    Union union = (Union)SetOperation.builder().build(k, Family.UNION);
    
    union.update(usk1);
    union.update(usk2);
    
    double exactUnionAnswer = k;
    
    CompactSketch comp1, comp2, comp3, comp4;
    double compEst;
    
    //test all the compacts
    comp1 = union.getResult(false, null); //ordered: false
    compEst = comp1.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.0);
    assertEquals(compEst, usk1est + usk2est/2, 0.0);
  
    comp2 = union.getResult(true, null);
    compEst = comp2.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.0);
    
    int bytes = comp2.getCurrentBytes(false);
    byte[] byteArray = new byte[bytes];
    Memory mem = new NativeMemory(byteArray);
    
    comp3 = union.getResult(false, mem);
    compEst = comp3.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.0);
    
    comp4 = union.getResult(true, mem);
    compEst = comp4.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.0);
  }

  @Test 
  public void checkHeapifyExact() {
    int lgK = 9; //512
    int k = 1 << lgK;

    UpdateSketch usk1 = UpdateSketch.builder().build(k);
    UpdateSketch usk2 = UpdateSketch.builder().build(k);
    
    for (int i=0; i<k/2; i++) usk1.update(i); //256
    for (int i=k/2; i<k; i++) usk2.update(i); //256 no overlap
    
    double usk1est = usk1.getEstimate();
    double usk2est = usk2.getEstimate();
    
    Union union = (Union)SetOperation.builder().build(k, Family.UNION);
    
    union.update(usk1);
    union.update(usk2);
    
    double exactUnionAnswer = k;
    byte[] byteArr1 = union.toByteArray();
    Memory srcMem = new NativeMemory(byteArr1);
    Union union2 = (Union)SetOperation.heapify(srcMem);
    
    CompactSketch comp1, comp2, comp3, comp4;
    double compEst;
    
    //test all the compacts
    comp1 = union2.getResult(false, null); //ordered: false
    compEst = comp1.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.0);
    assertEquals(compEst, usk1est + usk2est, 0.0);

    comp2 = union2.getResult(true, null); //ordered: true
    compEst = comp2.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.0);
    
    int bytes = comp2.getCurrentBytes(false);
    byte[] byteArr2 = new byte[bytes];
    Memory mem = new NativeMemory(byteArr2);
    
    comp3 = union2.getResult(false, mem);
    compEst = comp3.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.0);
    
    comp4 = union2.getResult(true, mem);
    compEst = comp4.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.0);
  }
  
  @Test
  public void checkHeapifyEstNoOverlap() {
    int lgK = 12; //4096
    int k = 1 << lgK;
    int u = 4*k;
    
    UpdateSketch usk1 = UpdateSketch.builder().build(k);
    UpdateSketch usk2 = UpdateSketch.builder().build(k);
    
    for (int i=0; i<u/2; i++) usk1.update(i); //2*k
    for (int i=u/2; i<u; i++) usk2.update(i); //2*k no overlap
    
//    double usk1est = usk1.getEstimate();
//    double usk2est = usk2.getEstimate();
    
    Union union = (Union)SetOperation.builder().build(k, Family.UNION);
    
    union.update(usk1);
    union.update(usk2);
    
    double exactUnionAnswer = u;
    
    byte[] byteArr1 = union.toByteArray();
    Memory srcMem = new NativeMemory(byteArr1);
    Union union2 = (Union)SetOperation.heapify(srcMem);
    
    CompactSketch comp1, comp2, comp3, comp4;
    double compEst;
    
  //test all the compacts
    comp1 = union2.getResult(false, null); //ordered: false
    compEst = comp1.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.05*u);
    
    comp2 = union2.getResult(true, null); //ordered: true
    compEst = comp2.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.05*u);
    
    int bytes = comp2.getCurrentBytes(false);
    byte[] byteArr2 = new byte[bytes];
    Memory mem = new NativeMemory(byteArr2);
    
    comp3 = union2.getResult(false, mem);
    compEst = comp3.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.05*u);
    
    comp4 = union2.getResult(true, mem);
    compEst = comp4.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.05*u);
  }
  
  @Test
  public void checkHeapifyEstNoOverlapOrderedIn() {
    int lgK = 12; //4096
    int k = 1 << lgK;
    int u = 4*k;
    
    UpdateSketch usk1 = UpdateSketch.builder().build(k);
    UpdateSketch usk2 = UpdateSketch.builder().build(k);
    
    for (int i=0; i<u/2; i++) usk1.update(i); //2*k
    for (int i=u/2; i<u; i++) usk2.update(i); //2*k no overlap
    
    CompactSketch compOrdered = usk2.compact(true, null);
    
    Union union = (Union)SetOperation.builder().build(k, Family.UNION);
    
    union.update(usk1);
    union.update(compOrdered);
    UpdateSketch emptySketch = UpdateSketch.builder().build(k);
    union.update(emptySketch);
    union.update(null);
    
    double exactUnionAnswer = u;
    
    byte[] byteArr1 = union.toByteArray();
    Memory uMem = new NativeMemory(byteArr1);
    Union union2 = (Union)SetOperation.heapify(uMem);
    
    CompactSketch comp1, comp2, comp3, comp4;
    double compEst;
    
    //test all the compacts
    comp1 = union2.getResult(false, null); //ordered: false
    compEst = comp1.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.05*u);
    
    comp2 = union2.getResult(true, null); //ordered: true
    compEst = comp2.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.05*u);
    
    int bytes = comp2.getCurrentBytes(false);
    byte[] byteArr2 = new byte[bytes];
    Memory mem = new NativeMemory(byteArr2);
    
    comp3 = union2.getResult(false, mem);
    compEst = comp3.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.05*u);
    
    comp4 = union2.getResult(true, mem);
    compEst = comp4.getEstimate();
    assertEquals(compEst, exactUnionAnswer, 0.05*u);
    
    union2.reset();
    assertEquals(union2.getResult(true, null).getEstimate(), 0.0, 0.0);
  }
  
  @Test
  public void printlnTest() {
    println("Test");
  }
  
  /**
   * @param s value to print
   */
  static void println(String s) {
    //System.out.println(s); //Disable here
  }
  
}