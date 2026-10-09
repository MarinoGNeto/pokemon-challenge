import { z } from 'zod'

/** Mirrors the backend's rules (User, RegisterUser) so most mistakes are caught before a request is sent. */
export const USERNAME_RULE =
  "Use 3–30 lowercase letters, digits, '.', '_' or '-', starting and ending with a letter or digit"

export const loginSchema = z.object({
  username: z.string().trim().min(1, 'Enter your username'),
  password: z.string().min(1, 'Enter your password'),
})

export const registerSchema = z.object({
  username: z
    .string()
    .trim()
    .toLowerCase()
    .min(3, USERNAME_RULE)
    .max(30, USERNAME_RULE)
    .regex(/^[a-z0-9]([a-z0-9._-]*[a-z0-9])?$/, USERNAME_RULE),
  email: z
    .string()
    .trim()
    .max(254, 'Use at most 254 characters')
    .regex(/^[^@\s]+@[^@\s]+\.[^@\s]+$/, 'Enter a valid email address'),
  password: z
    .string()
    .min(8, 'Use at least 8 characters')
    // BCrypt ignores everything after 72 bytes, so the server refuses longer passwords.
    .refine((value) => new TextEncoder().encode(value).length <= 72, 'Use at most 72 bytes'),
})

export type LoginValues = z.infer<typeof loginSchema>
export type RegisterValues = z.infer<typeof registerSchema>
