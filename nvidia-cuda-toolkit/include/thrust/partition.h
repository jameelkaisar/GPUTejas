/*
 *  Copyright 2008-2011 NVIDIA Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


/*! \file partition.h
 *  \brief Reorganizes a range based on a predicate
 */

#pragma once

#include <thrust/detail/config.h>
#include <thrust/pair.h>

namespace thrust
{

/*! \addtogroup reordering
 *  \ingroup algorithms
 *
 *  \addtogroup partitioning
 *  \ingroup reordering
 *  \{
 */

/*! \p partition reorders the elements <tt>[first, last)</tt> based on the function
 *  object \p pred, such that all of the elements that satisfy \p pred precede the
 *  elements that fail to satisfy it. The postcondition is that, for some iterator
 *  \c middle in the range <tt>[first, last)</tt>, <tt>pred(*i)</tt> is \c true for every
 *  iterator \c i in the range <tt>[first,middle)</tt> and \c false for every iterator
 *  \c i in the range <tt>[middle, last)</tt>. The return value of \p partition is
 *  \c middle.
 *
 *  Note that the relative order of elements in the two reordered sequences is not
 *  necessarily the same as it was in the original sequence. A different algorithm,
 *  \ref stable_partition, does guarantee to preserve the relative order.
 *
 *  \param first The beginning of the sequence to reorder.
 *  \param last The end of the sequence to reorder.
 *  \param pred A function object which decides to which partition each element of the
 *              sequence <tt>[first, last)</tt> belongs.
 *  \return An iterator referring to the first element of the second partition, that is,
 *          the sequence of the elements which do not satisfy \p pred.
 *
 *  \tparam ForwardIterator is a model of <a href="http://www.sgi.com/tech/stl/ForwardIterator.html">Forward Iterator</a>,
 *          and \p ForwardIterator's \c value_type is convertible to \p Predicate's \c argument_type,
 *          and \p ForwardIterator is mutable.
 *  \tparam Predicate is a model of <a href="http://www.sgi.com/tech/stl/Predicate.html">Predicate</a>.
 *
 *  The following code snippet demonstrates how to use \p partition to reorder a
 *  sequence so that even numbers precede odd numbers.
 *
 *  \code
 *  #include <thrust/partition.h>
 *  ...
 *  struct is_even
 *  {
 *    __host__ __device__
 *    bool operator()(const int &x)
 *    {
 *      return (x % 2) == 0;
 *    }
 *  };
 *  ...
 *  int A[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
 *  const int N = sizeof(A)/sizeof(int);
 *  thrust::partition(A, A + N,
 *                     is_even());
 *  // A is now {2, 4, 6, 8, 10, 1, 3, 5, 7, 9}
 *  \endcode
 *
 *  \see http://www.sgi.com/tech/stl/partition.html
 *  \see \p stable_partition
 *  \see \p partition_copy
 */
template<typename ForwardIterator,
         typename Predicate>
  ForwardIterator partition(ForwardIterator first,
                            ForwardIterator last,
                            Predicate pred);

/*! \p partition_copy differs from \ref partition only in that the reordered
 *  sequence is written to difference output sequences, rather than in place.
 *
 *  \p partition_copy copies the elements <tt>[first, last)</tt> based on the
 *  function object \p pred. All of the elements that satisfy \p pred are copied
 *  to the range beginning at \p out_true and all the elements that fail to satisfy it
 *  are copied to the range beginning at \p out_false.
 *
 *  \param first The beginning of the sequence to reorder.
 *  \param last The end of the sequence to reorder.
 *  \param out_true The destination of the resulting sequence of elements which satisfy \p pred.
 *  \param out_false The destination of the resulting sequence of elements which fail to satisfy \p pred.
 *  \param pred A function object which decides to which partition each element of the
 *              sequence <tt>[first, last)</tt> belongs.
 *  \return A \p pair p such that <tt>p.first</tt> is the end of the output range beginning
 *          at \p out_true and <tt>p.second</tt> is the end of the output range beginning at
 *          \p out_false.
 *
 *  \tparam InputIterator is a model of <a href="http://www.sgi.com/tech/stl/InputIterator.html">Input Iterator</a>,
 *          and \p InputIterator's \c value_type is convertible to \p Predicate's \c argument_type and \p InputIterator's \c value_type
 *          is convertible to \p OutputIterator1 and \p OutputIterator2's \c value_types.
 *  \tparam OutputIterator1 is a model of <a href="http://www.sgi.com/tech/stl/OutputIterator.html">Output Iterator</a>.
 *  \tparam OutputIterator2 is a model of <a href="http://www.sgi.com/tech/stl/OutputIterator.html">Output Iterator</a>.
 *  \tparam Predicate is a model of <a href="http://www.sgi.com/tech/stl/Predicate.html">Predicate</a>.
 *
 *  The following code snippet demonstrates how to use \p partition_copy to separate a
 *  sequence into two output sequences of even and odd numbers.
 *
 *  \code
 *  #include <thrust/partition.h>
 *  ...
 *  struct is_even
 *  {
 *    __host__ __device__
 *    bool operator()(const int &x)
 *    {
 *      return (x % 2) == 0;
 *    }
 *  };
 *  ...
 *  int A[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
 *  int result[10];
 *  const int N = sizeof(A)/sizeof(int);
 *  int *evens = result;
 *  int *odds  = result + 5;
 *  thrust::partition_copy(A, A + N, evens, odds, is_even());
 *  // A remains {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}
 *  // result is now {2, 4, 6, 8, 10, 1, 3, 5, 7, 9}
 *  // evens points to {2, 4, 6, 8, 10}
 *  // odds points to {1, 3, 5, 7, 9}
 *  \endcode
 *
 *  \note The relative order of elements in the two reordered sequences is not
 *  necessarily the same as it was in the original sequence. A different algorithm,
 *  \ref stable_partition_copy, does guarantee to preserve the relative order.
 *
 *  \see http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2008/n2569.pdf
 *  \see \p stable_partition_copy
 *  \see \p partition
 */
template<typename InputIterator,
         typename OutputIterator1,
         typename OutputIterator2,
         typename Predicate>
  thrust::pair<OutputIterator1,OutputIterator2>
    partition_copy(InputIterator first,
                   InputIterator last,
                   OutputIterator1 out_true,
                   OutputIterator2 out_false,
                   Predicate pred);

/*! \p stable_partition is much like \ref partition : it reorders the elements in the
 *  range <tt>[first, last)</tt> based on the function object \p pred, such that all of
 *  the elements that satisfy \p pred precede all of the elements that fail to satisfy
 *  it. The postcondition is that, for some iterator \p middle in the range
 *  <tt>[first, last)</tt>, <tt>pred(*i)</tt> is \c true for every iterator \c i in the
 *  range <tt>[first,middle)</tt> and \c false for every iterator \c i in the range
 *  <tt>[middle, last)</tt>. The return value of \p stable_partition is \c middle.
 *
 *  \p stable_partition differs from \ref partition in that \p stable_partition is
 *  guaranteed to preserve relative order. That is, if \c x and \c y are elements in
 *  <tt>[first, last)</tt>, such that <tt>pred(x) == pred(y)</tt>, and if \c x precedes
 *  \c y, then it will still be true after \p stable_partition that \c x precedes \c y.
 *
 *  \param first The first element of the sequence to reorder.
 *  \param last One position past the last element of the sequence to reorder.
 *  \param pred A function object which decides to which partition each element of the
 *              sequence <tt>[first, last)</tt> belongs.
 *  \return An iterator referring to the first element of the second partition, that is,
 *          the sequence of the elements which do not satisfy pred.
 *
 *  \tparam ForwardIterator is a model of <a href="http://www.sgi.com/tech/stl/ForwardIterator.html">Forward Iterator</a>,
 *          and \p ForwardIterator's \c value_type is convertible to \p Predicate's \c argument_type,
 *          and \p ForwardIterator is mutable.
 *  \tparam Predicate is a model of <a href="http://www.sgi.com/tech/stl/Predicate.html">Predicate</a>.
 *
 *  The following code snippet demonstrates how to use \p stable_partition to reorder a
 *  sequence so that even numbers precede odd numbers.
 *
 *  \code
 *  #include <thrust/partition.h>
 *  ...
 *  struct is_even
 *  {
 *    __host__ __device__
 *    bool operator()(const int &x)
 *    {
 *      return (x % 2) == 0;
 *    }
 *  };
 *  ...
 *  int A[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
 *  const int N = sizeof(A)/sizeof(int);
 *  thrust::stable_partition(A, A + N,
 *                            is_even());
 *  // A is now {2, 4, 6, 8, 10, 1, 3, 5, 7, 9}
 *  \endcode
 *
 *  \see http://www.sgi.com/tech/stl/stable_partition.html
 *  \see \p partition
 *  \see \p stable_partition_copy
 */
template<typename ForwardIterator,
         typename Predicate>
  ForwardIterator stable_partition(ForwardIterator first,
                                   ForwardIterator last,
                                   Predicate pred);


/*! \p stable_partition_copy differs from \ref stable_partition only in that the reordered
 *  sequence is written to difference output sequences, rather than in place.
 *
 *  \p stable_partition_copy copies the elements <tt>[first, last)</tt> based on the
 *  function object \p pred. All of the elements that satisfy \p pred are copied
 *  to the range beginning at \p out_true and all the elements that fail to satisfy it
 *  are copied to the range beginning at \p out_false.
 *
 *  \p stable_partition_copy differs from \ref partition_copy in that
 *  \p stable_partition_copy is guaranteed to preserve relative order. That is, if
 *  \c x and \c y are elements in <tt>[first, last)</tt>, such that
 *  <tt>pred(x) == pred(y)</tt>, and if \c x precedes \c y, then it will still be true
 *  after \p stable_partition_copy that \c x precedes \c y in the output.
 *
 *  \param first The first element of the sequence to reorder.
 *  \param last One position past the last element of the sequence to reorder.
 *  \param out_true The destination of the resulting sequence of elements which satisfy \p pred.
 *  \param out_false The destination of the resulting sequence of elements which fail to satisfy \p pred.
 *  \param pred A function object which decides to which partition each element of the
 *              sequence <tt>[first, last)</tt> belongs.
 *  \return A \p pair p such that <tt>p.first</tt> is the end of the output range beginning
 *          at \p out_true and <tt>p.second</tt> is the end of the output range beginning at
 *          \p out_false.
 *
 *  \tparam InputIterator is a model of <a href="http://www.sgi.com/tech/stl/InputIterator.html">Input Iterator</a>,
 *          and \p InputIterator's \c value_type is convertible to \p Predicate's \c argument_type and \p InputIterator's \c value_type
 *          is convertible to \p OutputIterator1 and \p OutputIterator2's \c value_types.
 *  \tparam OutputIterator1 is a model of <a href="http://www.sgi.com/tech/stl/OutputIterator.html">Output Iterator</a>.
 *  \tparam OutputIterator2 is a model of <a href="http://www.sgi.com/tech/stl/OutputIterator.html">Output Iterator</a>.
 *  \tparam Predicate is a model of <a href="http://www.sgi.com/tech/stl/Predicate.html">Predicate</a>.
 *
 *  The following code snippet demonstrates how to use \p stable_partition_copy to
 *  reorder a sequence so that even numbers precede odd numbers.
 *
 *  \code
 *  #include <thrust/partition.h>
 *  ...
 *  struct is_even
 *  {
 *    __host__ __device__
 *    bool operator()(const int &x)
 *    {
 *      return (x % 2) == 0;
 *    }
 *  };
 *  ...
 *  int A[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
 *  int result[10];
 *  const int N = sizeof(A)/sizeof(int);
 *  int *evens = result;
 *  int *odds  = result + 5;
 *  thrust::stable_partition_copy(A, A + N, evens, odds, is_even());
 *  // A remains {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}
 *  // result is now {2, 4, 6, 8, 10, 1, 3, 5, 7, 9}
 *  // evens points to {2, 4, 6, 8, 10}
 *  // odds points to {1, 3, 5, 7, 9}
 *  \endcode
 *
 *  \see http://www.open-std.org/jtc1/sc22/wg21/docs/papers/2008/n2569.pdf
 *  \see \p partition_copy
 *  \see \p stable_partition
 */
template<typename InputIterator,
         typename OutputIterator1,
         typename OutputIterator2,
         typename Predicate>
  thrust::pair<OutputIterator1,OutputIterator2>
    stable_partition_copy(InputIterator first,
                          InputIterator last,
                          OutputIterator1 out_true,
                          OutputIterator2 out_false,
                          Predicate pred);

/*! \} // end stream_compaction
 */

/*! \} // end reordering
 */

/*! \addtogroup searching
 *  \{
 */

/*! \p partition_point returns an iterator pointing to the end of the true
 *  partition of a partitioned range. \p partition_point requires the input range
 *  <tt>[first,last)</tt> to be a partition; that is, all elements which satisfy
 *  <tt>pred</tt> shall appear before those that do not.
 *  \param first The beginning of the range to consider.
 *  \param last The end of the range to consider.
 *  \param pred A function object which decides to which partition each element of the
 *              range <tt>[first, last)</tt> belongs.
 *  \return An iterator \c mid such that <tt>all_of(first, mid, pred)</tt>
 *          and <tt>none_of(mid, last, pred)</tt> are both true.
 *
 *  \tparam ForwardIterator is a model of <a href="http://www.sgi.com/tech/stl/ForwardIterator.html">Forward Iterator</a>,
 *          and \p ForwardIterator's \c value_type is convertible to \p Predicate's \c argument_type.
 *  \tparam Predicate is a model of <a href="http://www.sgi.com/tech/stl/Predicate.html">Predicate</a>.
 *
 *  \note Though similar, \p partition_point is not redundant with \p find_if_not.
 *        \p partition_point's precondition provides an opportunity for a
 *        faster implemention.
 *
 *  \code
 *  #include <thrust/partition.h>
 *
 *  struct is_even
 *  {
 *    __host__ __device__
 *    bool operator()(const int &x)
 *    {
 *      return (x % 2) == 0;
 *    }
 *  };
 *  
 *  ...
 *
 *  int A[] = {2, 4, 6, 8, 10, 1, 3, 5, 7, 9};
 *  int * B = thrust::partition_point(A, A + 10, is_even());
 *  // B - A is 5
 *  // [A, B) contains only even values
 *  \endcode
 *
 *  \see \p partition
 *  \see \p find_if_not
 */
template<typename ForwardIterator, typename Predicate>
  ForwardIterator partition_point(ForwardIterator first,
                                  ForwardIterator last,
                                  Predicate pred);
/*! \} // searching
 */

/*! \addtogroup reductions
 *  \{
 *  \addtogroup predicates
 *  \{
 */

/*! \p is_partitioned returns \c true if the given range 
 *  is partitioned with respect to a predicate, and \c false otherwise.
 *
 *  Specifically, \p is_partitioned returns \c true if <tt>[first, last)</tt>
 *  is empty of if <tt>[first, last)</tt> is partitioned by \p pred, i.e. if
 *  all elements that satisfy \p pred appear before those that do not.
 *
 *  \param first The beginning of the range to consider.
 *  \param last The end of the range to consider.
 *  \param pred A function object which decides to which partition each element of the
 *         range <tt>[first, last)</tt> belongs.
 *  \return \c true if the range <tt>[first, last)</tt> is partitioned with respect
 *          to \p pred, or if <tt>[first, last)</tt> is empty. \c false, otherwise.
 *
 *  \tparam InputIterator is a model of <a href="http://www.sgi.com/tech/stl/ForwardIterator.html">Input Iterator</a>,
 *          and \p InputIterator's \c value_type is convertible to \p Predicate's \c argument_type.
 *  \tparam Predicate is a model of <a href="http://www.sgi.com/tech/stl/Predicate.html">Predicate</a>.
 *  
 *  \code
 *  #include <thrust/partition.h>
 *
 *  struct is_even
 *  {
 *    __host__ __device__
 *    bool operator()(const int &x)
 *    {
 *      return (x % 2) == 0;
 *    }
 *  };
 *  
 *  ...
 *
 *  int A[] = {2, 4, 6, 8, 10, 1, 3, 5, 7, 9};
 *  int B[] = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
 *
 *  thrust::is_partitioned(A, A + 10); // returns true
 *  thrust::is_partitioned(B, B + 10); // returns false
 *  \endcode
 *
 *  \see \p partition
 */
template<typename InputIterator, typename Predicate>
  bool is_partitioned(InputIterator first,
                      InputIterator last,
                      Predicate pred);

/*! \} // end predicates
 *  \} // end reductions
 */

} // end thrust

#include <thrust/detail/partition.inl>

