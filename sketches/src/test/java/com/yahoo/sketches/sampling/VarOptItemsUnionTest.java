/*
 * Copyright 2017, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */
package com.yahoo.sketches.sampling;

import static com.yahoo.sketches.sampling.VarOptItemsSketchTest.EPS;
import static com.yahoo.sketches.sampling.VarOptItemsSketchTest.getUnweightedLongsVIS;
import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import com.yahoo.memory.Memory;
import com.yahoo.memory.NativeMemory;
import com.yahoo.sketches.ArrayOfLongsSerDe;
import com.yahoo.sketches.ArrayOfStringsSerDe;
import com.yahoo.sketches.Family;
import com.yahoo.sketches.SketchesArgumentException;


/**
 * @author Jon Malkin
 */
public class VarOptItemsUnionTest {
  @Test
  public void unionTwoExactSketches() {
    final int n = 4; // 2n < k
    final int k = 10;
    final VarOptItemsSketch<Integer> sk1 = VarOptItemsSketch.build(k);
    final VarOptItemsSketch<Integer> sk2 = VarOptItemsSketch.build(k);

    for (int i = 1; i <= n; ++i) {
      sk1.update(i, i);
      sk2.update(-i, i);
    }

    final VarOptItemsUnion<Integer> union = VarOptItemsUnion.build(k);
    union.update(sk1);
    union.update(sk2);

    final VarOptItemsSketch<Integer> result = union.getResult();
    assertEquals(result.getN(), 2 * n);
    assertEquals(result.getHRegionCount(), 2 * n);
    assertEquals(result.getRRegionCount(), 0);
  }

  @Test
  public void unionHeavySamplingSketch() {
    final int n1 = 20;
    final int k1 = 10;
    final int n2 = 6;
    final int k2 = 5;
    final VarOptItemsSketch<Integer> sk1 = VarOptItemsSketch.build(k1);
    final VarOptItemsSketch<Integer> sk2 = VarOptItemsSketch.build(k2);

    for (int i = 1; i <= n1; ++i) {
      sk1.update(i, i);
    }

    for (int i = 1; i < n2; ++i) { // we'll add a very heavy one later
      sk2.update(-i, i + 1000.0);
    }
    sk2.update(-n2, 1000000.0);

    final VarOptItemsUnion<Integer> union = VarOptItemsUnion.build(k1);
    union.update(sk1);
    union.update(sk2);

    final VarOptItemsSketch<Integer> result = union.getResult();
    assertEquals(result.getN(), n1 + n2);
    assertEquals(result.getK(), k2); // heavy enough it'll pull back to k2
    assertEquals(result.getHRegionCount(), 1);
    assertEquals(result.getRRegionCount(), k2 - 1);
  }

  @Test
  public void unionIdenticalSamplingSketches() {
    final int k = 20;
    final int n = 50;
    VarOptItemsSketch<Long> sketch = getUnweightedLongsVIS(k, n);

    final VarOptItemsUnion<Long> union = VarOptItemsUnion.build(k);
    union.update(sketch);
    union.update(sketch);

    VarOptItemsSketch<Long> result = union.getResult();
    double expectedWeight = 2.0 * n; // unweighted, aka uniform weight of 1.0
    assertEquals(result.getN(), 2 * n);
    assertEquals(result.getTotalWtR(), expectedWeight);

    // add another sketch, such that sketchTau < outerTau
    sketch = getUnweightedLongsVIS(k, k + 1); // tau = (k + 1) / k
    union.update(sketch);
    result = union.getResult();
    expectedWeight = 2.0 * n + k + 1;
    assertEquals(result.getN(), 2 * n + k + 1);
    assertEquals(result.getTotalWtR(), expectedWeight, EPS);
  }

  @Test
  public void unionSmallSamplingSketch() {
    final int kSmall = 16;
    final int n1 = 32;
    final int n2 = 64;
    final int kMax = 128;

    // small k sketch, but sampling
    VarOptItemsSketch<Long> sketch = getUnweightedLongsVIS(kSmall, n1);
    sketch.update(-1L, n1 ^ 2); // add a heavy item

    final VarOptItemsUnion<Long> union = VarOptItemsUnion.build(kMax);
    union.update(sketch);

    // another one, but different n to get a different per-item weight
    sketch = getUnweightedLongsVIS(kSmall, n2);
    union.update(sketch);

    // should trigger migrateMarkedItemsByDecreasingK()
    final VarOptItemsSketch<Long> result = union.getResult();
    assertEquals(result.getN(), n1 + n2 + 1);
    assertEquals(result.getTotalWtR(), 96.0, EPS); // n1+n2 light items, ignore the heavy one
  }

  @Test
  public void serializeEmptyUnion() {
    final int k = 100;
    final VarOptItemsUnion<String> union = VarOptItemsUnion.build(k);

    final ArrayOfStringsSerDe serDe = new ArrayOfStringsSerDe();
    final byte[] bytes = union.toByteArray(serDe);
    assertEquals(bytes.length, 8);

    final NativeMemory mem = new NativeMemory(bytes);
    final VarOptItemsUnion<String> rebuilt = VarOptItemsUnion.heapify(mem, serDe);

    final VarOptItemsSketch<String> sketch = rebuilt.getResult();
    assertEquals(sketch.getN(), 0);
  }

  @Test
  public void serializeExactUnion() {
    final int n1 = 32;
    final int n2 = 64;
    final int k = 128;
    final VarOptItemsSketch<Long> sketch1 = getUnweightedLongsVIS(k, n1);
    final VarOptItemsSketch<Long> sketch2 = getUnweightedLongsVIS(k, n2);

    final VarOptItemsUnion<Long> union = VarOptItemsUnion.build(k);
    union.update(sketch1);
    union.update(sketch2);

    final ArrayOfLongsSerDe serDe = new ArrayOfLongsSerDe();
    final byte[] unionBytes = union.toByteArray(serDe);
    final NativeMemory mem = new NativeMemory(unionBytes);

    final VarOptItemsUnion<Long> rebuilt = VarOptItemsUnion.heapify(mem, serDe);
    compareUnions(rebuilt, union);
  }


  @Test
  public void serializeSamplingUnion() {
    final int n = 256;
    final int k = 128;
    final VarOptItemsSketch<Long> sketch = getUnweightedLongsVIS(k, n);
    sketch.update(n + 1L, 1000.0);
    sketch.update(n + 2L, 1001.0);

    final VarOptItemsUnion<Long> union = VarOptItemsUnion.build(k);
    union.update(sketch);

    final ArrayOfLongsSerDe serDe = new ArrayOfLongsSerDe();
    final byte[] unionBytes = union.toByteArray(serDe);
    final NativeMemory mem = new NativeMemory(unionBytes);

    final VarOptItemsUnion<Long> rebuilt = VarOptItemsUnion.heapify(mem, serDe);
    compareUnions(rebuilt, union);
  }


  static <T> void compareUnions(final VarOptItemsUnion<T> u1,
                                final VarOptItemsUnion<T> u2) {
    assertEquals(u1.getOuterTau(), u2.getOuterTau());

    final VarOptItemsSamples<T> s1 = u1.getResult().getSketchSamples();
    final VarOptItemsSamples<T> s2 = u2.getResult().getSketchSamples();

    assertEquals(s1.getNumSamples(), s2.getNumSamples());
    assertEquals(s1.items(), s2.items());
    assertEquals(s1.weights(), s2.weights());
  }

}
