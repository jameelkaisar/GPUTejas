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

#pragma once

#include <thrust/iterator/iterator_traits.h>
#include <thrust/detail/backend/cuda/merge.h>
#include <thrust/detail/backend/omp/merge.h>

namespace thrust
{

namespace detail
{

namespace backend
{

namespace dispatch
{

template<typename InputIterator1,
         typename InputIterator2,
         typename OutputIterator,
         typename StrictWeakOrdering>
  OutputIterator merge(InputIterator1 first1,
                       InputIterator1 last1,
                       InputIterator2 first2,
                       InputIterator2 last2,
                       OutputIterator result,
                       StrictWeakOrdering comp,
                       thrust::detail::omp_device_space_tag)
{
  // omp backend
  return thrust::detail::backend::omp::merge(first1,last1,first2,last2,result,comp);
} // end merge()


template<typename InputIterator1,
         typename InputIterator2,
         typename OutputIterator,
         typename StrictWeakOrdering>
  OutputIterator merge(InputIterator1 first1,
                       InputIterator1 last1,
                       InputIterator2 first2,
                       InputIterator2 last2,
                       OutputIterator result,
                       StrictWeakOrdering comp,
                       thrust::detail::cuda_device_space_tag)
{
  // CUDA backend
  return thrust::detail::backend::cuda::merge(first1,last1,first2,last2,result,comp);
} // end merge()


} // end dispatch

} // end backend

} // end detail

} // end thrust

