// Sweeps S3 when a note's DynamoDB item is removed (TTL expiry, user
// delete, or admin takedown): deletes the content body and any premium
// images it referenced, then evicts those images from CloudFront.
//
// Triggered by the notes table's stream, filtered to REMOVE events.

import {
  S3Client,
  GetObjectCommand,
  DeleteObjectsCommand,
} from '@aws-sdk/client-s3'
import {
  CloudFrontClient,
  CreateInvalidationCommand,
} from '@aws-sdk/client-cloudfront'

const s3 = new S3Client({})
const cloudfront = new CloudFrontClient({})

const BUCKET = process.env.BUCKET
const DISTRIBUTION_ID = process.env.DISTRIBUTION_ID

// Image URLs embedded in note content: /images/{userId}/{uuid}.{ext}
const IMAGE_KEY = /\/(images\/[A-Za-z0-9_-]+\/[A-Za-z0-9-]+\.(?:png|jpe?g|webp|gif))/g

export const handler = async (event) => {
  for (const record of event.Records ?? []) {
    if (record.eventName !== 'REMOVE') continue
    const slug = record.dynamodb?.Keys?.slug?.S
    if (!slug) continue

    const bodyKey = `notes/${slug}.json`
    let content = null
    try {
      const body = await s3.send(
        new GetObjectCommand({ Bucket: BUCKET, Key: bodyKey })
      )
      content = await body.Body.transformToString()
    } catch (e) {
      // Already cleaned up synchronously (admin takedown) — nothing to sweep.
      if (e.name !== 'NoSuchKey') throw e
      console.log(`note ${slug}: body already gone, nothing to sweep`)
      continue
    }

    const imageKeys = [
      ...new Set([...content.matchAll(IMAGE_KEY)].map((m) => m[1])),
    ]
    await s3.send(
      new DeleteObjectsCommand({
        Bucket: BUCKET,
        Delete: {
          Objects: [bodyKey, ...imageKeys].map((Key) => ({ Key })),
          Quiet: true,
        },
      })
    )
    if (imageKeys.length > 0 && DISTRIBUTION_ID) {
      await cloudfront.send(
        new CreateInvalidationCommand({
          DistributionId: DISTRIBUTION_ID,
          InvalidationBatch: {
            CallerReference: `cleanup-${slug}-${Date.now()}`,
            Paths: {
              Quantity: imageKeys.length,
              Items: imageKeys.map((key) => `/${key}`),
            },
          },
        })
      )
    }
    console.log(`note ${slug}: swept body + ${imageKeys.length} image(s)`)
  }
}
