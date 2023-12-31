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


/*! \file fill.h
 *  \brief Device implementation of fill.
 */

#pragma once

#include <thrust/detail/internal_functional.h>
#include <thrust/generate.h>

namespace thrust
{
namespace detail
{
namespace backend
{
namespace generic
{


template<typename OutputIterator, typename Size, typename T>
  OutputIterator fill_n(OutputIterator first,
                        Size n,
                        const T &value)
{
  return thrust::generate_n(first, n, thrust::detail::fill_functor<T>(value));
}

template<typename ForwardIterator, typename T>
  void fill(ForwardIterator first,
            ForwardIterator last,
            const T &value)
{
  // this is safe because we know ForwardIterator is random access at this point
  thrust::detail::backend::fill_n(first, thrust::distance(first,last), value);
}


} // end namespace generic
} // end namespace backend
} // end namespace detail
} // end namespace thrust

