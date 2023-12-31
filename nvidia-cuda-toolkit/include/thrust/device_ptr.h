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


/*! \file device_ptr.h
 *  \brief A pointer to a variable which resides in the "device" memory space
 */

#pragma once

#include <thrust/detail/config.h>
#include <thrust/detail/pointer_base.h>
#include <thrust/detail/type_traits.h>
#include <thrust/detail/type_traits/pointer_traits.h>
#include <ostream>

namespace thrust
{

/*! \addtogroup memory_management Memory Management
 *  \addtogroup memory_management_classes Memory Management Classes
 *  \ingroup memory_management
 *  \{
 */

// forward declarations
template<typename T> class device_reference;

/*! \p device_ptr stores a pointer to an object allocated in device memory. This type
 *  provides type safety when dispatching standard algorithms on ranges resident in
 *  device memory.
 *
 *  \p device_ptr has pointer semantics: it may be dereferenced safely from the host and
 *  may be manipulated with pointer arithmetic.
 *
 *  \p device_ptr can be created with the functions device_malloc, device_new, or
 *  device_pointer_cast, or by explicitly calling its constructor with a raw pointer.
 *
 *  The raw pointer encapsulated by a \p device_ptr may be obtained by either its <tt>get</tt>
 *  method or the \p raw_pointer_cast free function.
 *
 *  \note \p device_ptr is not a smart pointer; it is the programmer's responsibility to
 *  deallocate memory pointed to by \p device_ptr.
 *
 *  \see device_malloc
 *  \see device_new
 *  \see device_pointer_cast
 *  \see raw_pointer_cast
 */
template<typename T>
  class device_ptr
    : public thrust::detail::pointer_base<
               thrust::device_ptr<T>,
               T,
               thrust::device_reference<T>,
               thrust::detail::default_device_space_tag
             >
{
  private:
    typedef thrust::detail::pointer_base<
      thrust::device_ptr<T>,
      T,
      thrust::device_reference<T>,
      thrust::detail::default_device_space_tag
    > super_t;

  public:
    /*! \p device_ptr's null constructor initializes its raw pointer to \c 0.
     */
    __host__ __device__
    device_ptr() : super_t() {}

    /*! \p device_ptr's copy constructor is templated to allow copying to a
     *  <tt>device_ptr<const T></tt> from a <tt>T *</tt>.
     *  
     *  \param ptr A raw pointer to copy from, presumed to point to a location in
     *         device memory.
     */
    template<typename OtherT>
    __host__ __device__
    explicit device_ptr(OtherT *ptr) : super_t(ptr) {}

    /*! \p device_ptr's copy constructor allows copying from another device_ptr with related type.
     *  \param other The \p device_ptr to copy from.
     */
    template<typename OtherT>
    __host__ __device__
    device_ptr(const device_ptr<OtherT> &other) : super_t(other) {}

    /*! \p device_ptr's assignment operator allows assigning from another \p device_ptr with related type.
     *  \param other The other \p device_ptr to copy from.
     *  \return <tt>*this</tt>
     */
    template<typename OtherT>
    __host__ __device__
    device_ptr &operator=(const device_ptr<OtherT> &other)
    {
      super_t::operator=(other);
      return *this;
    }

// declare these members for the purpose of Doxygenating them
// they actually exist in a derived-from class
#if 0
    /*! This method returns this \p device_ptr's raw pointer.
     *  \return This \p device_ptr's raw pointer.
     */
    __host__ __device__
    T *get(void) const;
#endif // end doxygen-only members
}; // end device_ptr

/*! This operator outputs the value of a \p device_ptr's raw pointer to a \p std::basic_ostream.
 *
 *  \param os The std::basic_ostream of interest.
 *  \param p The device_ptr of interest.
 *  \return os.
 */
template<class E, class T, class Y>
inline std::basic_ostream<E, T> &operator<<(std::basic_ostream<E, T> &os, const device_ptr<Y> &p);

/*! \}
 */


/*!
 *  \addtogroup memory_management_functions Memory Management Functions
 *  \ingroup memory_management
 *  \{
 */

/*! \p device_pointer_cast creates a device_ptr from a raw pointer which is presumed to point
 *  to a location in device memory.
 *
 *  \param ptr A raw pointer, presumed to point to a location in device memory.
 *  \return A device_ptr wrapping ptr.
 */
template<typename T>
__host__ __device__
inline device_ptr<T> device_pointer_cast(T *ptr);

/*! This version of \p device_pointer_cast creates a copy of a device_ptr from another device_ptr.
 *  This version is included for symmetry with \p raw_pointer_cast.
 *
 *  \param ptr A device_ptr.
 *  \return A copy of \p ptr.
 */
template<typename T>
__host__ __device__
inline device_ptr<T> device_pointer_cast(const device_ptr<T> &ptr);

/*! \p raw_pointer_cast creates a "raw" pointer from a pointer-like type,
 *  simply returning the wrapped pointer, should it exist.
 *
 *  \param ptr The pointer of interest.
 *  \return <tt>ptr.get()</tt>, if the expression is well formed; <tt>ptr</tt>, otherwise.
 */
template<typename Pointer>
__host__ __device__
inline typename thrust::detail::pointer_traits<Pointer>::raw_pointer
  raw_pointer_cast(const Pointer &ptr);

/*! \}
 */

} // end thrust

#include <thrust/detail/device_ptr.inl>

