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


/*! \file distance.h
 *  \brief Device implementations for distance.
 */

#pragma once

#include <thrust/pair.h>
#include <thrust/reduce.h>
#include <thrust/transform_reduce.h>

#include <thrust/iterator/iterator_traits.h>
#include <thrust/iterator/counting_iterator.h>
#include <thrust/iterator/zip_iterator.h>

namespace thrust
{
namespace detail
{
namespace backend
{
namespace generic
{
namespace detail
{

//////////////
// Functors //
//////////////

// return the smaller/larger element making sure to prefer the 
// first occurance of the minimum/maximum element
template <typename InputType, typename IndexType, typename BinaryPredicate>
struct min_element_reduction
{
  BinaryPredicate comp;

  __host__ __device__ 
  min_element_reduction(BinaryPredicate comp) : comp(comp){}

  __host__ __device__ 
  thrust::tuple<InputType, IndexType>
  operator()(const thrust::tuple<InputType, IndexType>& lhs, 
             const thrust::tuple<InputType, IndexType>& rhs )
  {
    if(comp(thrust::get<0>(lhs), thrust::get<0>(rhs)))
      return lhs;
    if(comp(thrust::get<0>(rhs), thrust::get<0>(lhs)))
      return rhs;

    // values are equivalent, prefer value with smaller index
    if(thrust::get<1>(lhs) < thrust::get<1>(rhs))
      return lhs;
    else
      return rhs;
  } // end operator()()

}; // end min_element_reduction


template <typename InputType, typename IndexType, typename BinaryPredicate>
struct max_element_reduction
{
  BinaryPredicate comp;

  __host__ __device__ 
  max_element_reduction(BinaryPredicate comp) : comp(comp){}

  __host__ __device__ 
  thrust::tuple<InputType, IndexType>
  operator()(const thrust::tuple<InputType, IndexType>& lhs, 
             const thrust::tuple<InputType, IndexType>& rhs )
  {
    if(comp(thrust::get<0>(lhs), thrust::get<0>(rhs)))
      return rhs;
    if(comp(thrust::get<0>(rhs), thrust::get<0>(lhs)))
      return lhs;

    // values are equivalent, prefer value with smaller index
    if(thrust::get<1>(lhs) < thrust::get<1>(rhs))
      return lhs;
    else
      return rhs;
  } // end operator()()

}; // end max_element_reduction

// return the smaller & larger element making sure to prefer the 
// first occurance of the minimum/maximum element
template <typename InputType, typename IndexType, typename BinaryPredicate>
struct minmax_element_reduction
{
  BinaryPredicate comp;

  minmax_element_reduction(BinaryPredicate comp) : comp(comp){}

  __host__ __device__ 
  thrust::tuple< thrust::tuple<InputType,IndexType>, thrust::tuple<InputType,IndexType> >
  operator()(const thrust::tuple< thrust::tuple<InputType,IndexType>, thrust::tuple<InputType,IndexType> >& lhs, 
             const thrust::tuple< thrust::tuple<InputType,IndexType>, thrust::tuple<InputType,IndexType> >& rhs )
  {

    return thrust::make_tuple(min_element_reduction<InputType, IndexType, BinaryPredicate>(comp)(thrust::get<0>(lhs), thrust::get<0>(rhs)),
                              max_element_reduction<InputType, IndexType, BinaryPredicate>(comp)(thrust::get<1>(lhs), thrust::get<1>(rhs)));
  } // end operator()()
}; // end minmax_element_reduction

template <typename InputType, typename IndexType>
struct duplicate_tuple
{
  __host__ __device__ 
  thrust::tuple< thrust::tuple<InputType,IndexType>, thrust::tuple<InputType,IndexType> >
  operator()(const thrust::tuple<InputType,IndexType>& t)
  {
    return thrust::make_tuple(t, t);
  }
}; // end duplicate_tuple

} // end namespace detail


template <typename ForwardIterator, typename BinaryPredicate>
ForwardIterator min_element(ForwardIterator first,
                            ForwardIterator last,
                            BinaryPredicate comp)
{
  if (first == last)
    return last;

  typedef typename thrust::iterator_traits<ForwardIterator>::value_type      InputType;
  typedef typename thrust::iterator_traits<ForwardIterator>::difference_type IndexType;

  thrust::tuple<InputType, IndexType> result =
    thrust::reduce
      (thrust::make_zip_iterator(thrust::make_tuple(first, thrust::counting_iterator<IndexType>(0))),
       thrust::make_zip_iterator(thrust::make_tuple(first, thrust::counting_iterator<IndexType>(0))) + (last - first),
       thrust::tuple<InputType, IndexType>(*first, 0),
       detail::min_element_reduction<InputType, IndexType, BinaryPredicate>(comp));

  return first + thrust::get<1>(result);
} // end min_element()

template <typename ForwardIterator, typename BinaryPredicate>
ForwardIterator max_element(ForwardIterator first,
                            ForwardIterator last,
                            BinaryPredicate comp)
{
  if (first == last)
    return last;

  typedef typename thrust::iterator_traits<ForwardIterator>::value_type      InputType;
  typedef typename thrust::iterator_traits<ForwardIterator>::difference_type IndexType;

  thrust::tuple<InputType, IndexType> result =
    thrust::reduce
      (thrust::make_zip_iterator(thrust::make_tuple(first, thrust::counting_iterator<IndexType>(0))),
       thrust::make_zip_iterator(thrust::make_tuple(first, thrust::counting_iterator<IndexType>(0))) + (last - first),
       thrust::tuple<InputType, IndexType>(*first, 0),
       detail::max_element_reduction<InputType, IndexType, BinaryPredicate>(comp));

  return first + thrust::get<1>(result);
} // end max_element()

template <typename ForwardIterator, typename BinaryPredicate>
thrust::pair<ForwardIterator,ForwardIterator> minmax_element(ForwardIterator first, 
                                                             ForwardIterator last,
                                                             BinaryPredicate comp)
{
  if (first == last)
    return thrust::make_pair(last, last);

  typedef typename thrust::iterator_traits<ForwardIterator>::value_type      InputType;
  typedef typename thrust::iterator_traits<ForwardIterator>::difference_type IndexType;

  thrust::tuple< thrust::tuple<InputType,IndexType>, thrust::tuple<InputType,IndexType> > result = 
    thrust::transform_reduce
      (thrust::make_zip_iterator(thrust::make_tuple(first, thrust::counting_iterator<IndexType>(0))),
       thrust::make_zip_iterator(thrust::make_tuple(first, thrust::counting_iterator<IndexType>(0))) + (last - first),
       detail::duplicate_tuple<InputType, IndexType>(),
       detail::duplicate_tuple<InputType, IndexType>()(thrust::tuple<InputType, IndexType>(*first, 0)),
       detail::minmax_element_reduction<InputType, IndexType, BinaryPredicate>(comp));

  return thrust::make_pair(first + thrust::get<1>(thrust::get<0>(result)), first + thrust::get<1>(thrust::get<1>(result)));
} // end minmax_element()

} // end namespace generic
} // end namespace backend
} // end namespace detail
} // end namespace thrust

