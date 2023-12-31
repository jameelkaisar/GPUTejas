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


/*! \file generate.inl
 *  \author Jared Hoberock
 *  \brief Inline file for generate.h.
 */

#include <thrust/generate.h>
#include <thrust/for_each.h>
#include <thrust/detail/type_traits.h>
#include <thrust/iterator/iterator_traits.h>
#include <thrust/detail/internal_functional.h>

namespace thrust
{

template<typename ForwardIterator,
         typename Generator>
  void generate(ForwardIterator first,
                ForwardIterator last,
                Generator gen)
{
  typedef typename thrust::iterator_space<ForwardIterator>::type Space;
  return thrust::for_each(first, last, typename detail::generate_functor<Space,Generator>::type(gen));
} // end generate()


template<typename OutputIterator,
         typename Size,
         typename Generator>
  OutputIterator generate_n(OutputIterator first,
                            Size n,
                            Generator gen)
{
  typedef typename thrust::iterator_space<OutputIterator>::type Space;
  return detail::for_each_n(first, n, typename detail::generate_functor<Space,Generator>::type(gen));
} // end generate()


} // end thrust

